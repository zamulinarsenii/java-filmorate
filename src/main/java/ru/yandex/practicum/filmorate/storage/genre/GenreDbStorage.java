package ru.yandex.practicum.filmorate.storage.genre;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.List;

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
}
