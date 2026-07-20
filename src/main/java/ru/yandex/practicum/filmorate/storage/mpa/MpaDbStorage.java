package ru.yandex.practicum.filmorate.storage.mpa;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class MpaDbStorage implements MpaStorage {
    private static final RowMapper<Mpa> MPA_MAPPER = (rs, rowNum) ->
            new Mpa(rs.getInt("mpa_id"), rs.getString("name"));

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<Mpa> findAll() {
        return jdbcTemplate.query("SELECT * FROM mpa_ratings ORDER BY mpa_id", MPA_MAPPER);
    }

    @Override
    public Mpa findById(long id) {
        return jdbcTemplate.query("SELECT * FROM mpa_ratings WHERE mpa_id = ?", MPA_MAPPER, id)
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Рейтинг MPA с id " + id + " не найден"));
    }
}
