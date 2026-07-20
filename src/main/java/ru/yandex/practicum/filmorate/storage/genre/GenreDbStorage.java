package ru.yandex.practicum.filmorate.storage.genre;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class GenreDbStorage implements GenreStorage {
    private static final RowMapper<Genre> GENRE_MAPPER = (rs, rowNum) ->
            new Genre(rs.getInt("genre_id"), rs.getString("name"));

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<Genre> findAll() {
        return jdbcTemplate.query("SELECT * FROM genres ORDER BY genre_id", GENRE_MAPPER);
    }

    @Override
    public Genre findById(long id) {
        return jdbcTemplate.query("SELECT * FROM genres WHERE genre_id = ?", GENRE_MAPPER, id)
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Жанр с id " + id + " не найден"));
    }

    @Override
    public List<Genre> findByIds(Collection<Integer> ids) {
        Set<Integer> uniqueIds = new LinkedHashSet<>(ids);
        if (uniqueIds.isEmpty()) {
            return List.of();
        }

        String placeholders = String.join(", ", Collections.nCopies(uniqueIds.size(), "?"));
        List<Genre> genres = jdbcTemplate.query(
                "SELECT * FROM genres WHERE genre_id IN (" + placeholders + ") ORDER BY genre_id",
                GENRE_MAPPER,
                uniqueIds.toArray()
        );

        if (genres.size() != uniqueIds.size()) {
            Set<Integer> foundIds = new LinkedHashSet<>();
            genres.forEach(genre -> foundIds.add(genre.getId()));
            Integer missingId = uniqueIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .findFirst()
                    .orElseThrow();
            throw new NotFoundException("Жанр с id " + missingId + " не найден");
        }
        return genres;
    }
}
