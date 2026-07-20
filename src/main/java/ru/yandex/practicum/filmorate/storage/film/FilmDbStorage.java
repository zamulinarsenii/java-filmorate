package ru.yandex.practicum.filmorate.storage.film;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Primary
@Repository("filmDbStorage")
@RequiredArgsConstructor
public class FilmDbStorage implements FilmStorage {
    private static final String SELECT_FILMS = """
            SELECT f.film_id, f.name, f.description, f.release_date, f.duration,
                   m.mpa_id, m.name AS mpa_name
            FROM films f
            LEFT JOIN mpa_ratings m ON m.mpa_id = f.mpa_id
            """;

    private static final RowMapper<Film> FILM_MAPPER = (rs, rowNum) -> {
        Film film = new Film();
        film.setId(rs.getLong("film_id"));
        film.setName(rs.getString("name"));
        film.setDescription(rs.getString("description"));
        film.setReleaseDate(rs.getDate("release_date") == null
                ? null : rs.getDate("release_date").toLocalDate());
        film.setDuration(rs.getInt("duration"));
        Integer mpaId = rs.getObject("mpa_id", Integer.class);
        if (mpaId != null) {
            film.setMpa(new Mpa(mpaId, rs.getString("mpa_name")));
        }
        return film;
    };

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<Film> findAll() {
        List<Film> films = jdbcTemplate.query(SELECT_FILMS + " ORDER BY f.film_id", FILM_MAPPER);
        loadGenres(films);
        return films;
    }

    @Override
    public Film findById(long id) {
        Film film = jdbcTemplate.query(SELECT_FILMS + " WHERE f.film_id = ?", FILM_MAPPER, id)
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Фильм с id " + id + " не найден"));
        loadGenres(List.of(film));
        return film;
    }

    @Override
    @Transactional
    public Film create(Film film) {
        String sql = """
                INSERT INTO films (name, description, release_date, duration, mpa_id)
                VALUES (?, ?, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, film.getName());
            statement.setString(2, film.getDescription());
            statement.setDate(3, film.getReleaseDate() == null ? null : Date.valueOf(film.getReleaseDate()));
            statement.setInt(4, film.getDuration());
            if (film.getMpa() == null || film.getMpa().getId() == null) {
                statement.setNull(5, Types.INTEGER);
            } else {
                statement.setInt(5, film.getMpa().getId());
            }
            return statement;
        }, keyHolder);
        film.setId(Objects.requireNonNull(keyHolder.getKey()).longValue());
        saveGenres(film);
        return findById(film.getId());
    }

    @Override
    @Transactional
    public Film update(Film film) {
        Integer mpaId = film.getMpa() == null ? null : film.getMpa().getId();
        int updated = jdbcTemplate.update("""
                UPDATE films
                SET name = ?, description = ?, release_date = ?, duration = ?, mpa_id = ?
                WHERE film_id = ?
                """, film.getName(), film.getDescription(), film.getReleaseDate(),
                film.getDuration(), mpaId, film.getId());
        if (updated == 0) {
            throw new NotFoundException("Фильм с id " + film.getId() + " не найден");
        }
        jdbcTemplate.update("DELETE FROM film_genres WHERE film_id = ?", film.getId());
        saveGenres(film);
        return findById(film.getId());
    }

    @Override
    public void addLike(Film film, User user) {
        jdbcTemplate.update("MERGE INTO film_likes (film_id, user_id) KEY (film_id, user_id) VALUES (?, ?)",
                film.getId(), user.getId());
    }

    @Override
    public boolean removeLike(Film film, User user) {
        return jdbcTemplate.update("DELETE FROM film_likes WHERE film_id = ? AND user_id = ?",
                film.getId(), user.getId()) > 0;
    }

    @Override
    public List<Film> getPopularFilms(int count) {
        List<Film> films = jdbcTemplate.query(SELECT_FILMS + """
                ORDER BY (SELECT COUNT(*) FROM film_likes fl WHERE fl.film_id = f.film_id) DESC,
                         f.film_id
                LIMIT ?
                """, FILM_MAPPER, count);
        loadGenres(films);
        return films;
    }

    private void loadGenres(List<Film> films) {
        if (films.isEmpty()) {
            return;
        }

        Map<Long, Film> filmsById = new LinkedHashMap<>();
        films.forEach(film -> {
            film.setGenres(new LinkedHashSet<>());
            filmsById.put(film.getId(), film);
        });

        String placeholders = String.join(", ", Collections.nCopies(filmsById.size(), "?"));
        jdbcTemplate.query("""
                SELECT fg.film_id, g.genre_id, g.name
                FROM genres g
                JOIN film_genres fg ON fg.genre_id = g.genre_id
                WHERE fg.film_id IN (%s)
                ORDER BY fg.film_id, g.genre_id
                """.formatted(placeholders), resultSet -> {
            Film film = filmsById.get(resultSet.getLong("film_id"));
            if (film != null) {
                film.getGenres().add(new Genre(
                        resultSet.getInt("genre_id"),
                        resultSet.getString("name")
                ));
            }
        }, filmsById.keySet().toArray());
    }

    private void saveGenres(Film film) {
        Set<Genre> genres = film.getGenres();
        if (genres == null || genres.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate(
                "MERGE INTO film_genres (film_id, genre_id) KEY (film_id, genre_id) VALUES (?, ?)",
                genres,
                genres.size(),
                (statement, genre) -> {
                    statement.setLong(1, film.getId());
                    statement.setInt(2, genre.getId());
                }
        );
    }
}
