package ru.yandex.practicum.filmorate.storage.film;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JdbcTest
@AutoConfigureTestDatabase
@Import(FilmDbStorage.class)
class FilmDbStorageTest {
    @Autowired
    private FilmDbStorage filmStorage;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldCreateFindUpdateAndListFilmsWithReferences() {
        Film film = createFilm("First", 1, 1, 2);

        assertThat(film.getId()).isPositive();
        assertThat(film.getMpa().getName()).isEqualTo("G");
        assertThat(film.getGenres()).extracting(Genre::getId).containsExactly(1, 2);

        film.setName("Updated");
        film.setMpa(new Mpa(2, null));
        film.setGenres(new LinkedHashSet<>(List.of(new Genre(3, null))));
        Film updated = filmStorage.update(film);

        assertThat(updated.getName()).isEqualTo("Updated");
        assertThat(updated.getMpa().getName()).isEqualTo("PG");
        assertThat(updated.getGenres()).extracting(Genre::getId).containsExactly(3);
        assertThat(filmStorage.findAll()).extracting(Film::getId).contains(updated.getId());
    }

    @Test
    void shouldManageLikesAndReturnPopularFilms() {
        Film first = createFilm("First", 1, 1);
        Film second = createFilm("Second", 2, 2);
        long firstUser = createUser("first@test.ru", "first");
        long secondUser = createUser("second@test.ru", "second");

        filmStorage.addLike(second.getId(), firstUser);
        filmStorage.addLike(second.getId(), secondUser);
        filmStorage.addLike(first.getId(), firstUser);

        assertThat(filmStorage.getPopularFilms(2))
                .extracting(Film::getId)
                .containsExactly(second.getId(), first.getId());
        assertThat(filmStorage.removeLike(second.getId(), secondUser)).isTrue();
        assertThat(filmStorage.removeLike(second.getId(), secondUser)).isFalse();
    }

    @Test
    void shouldThrowWhenFilmDoesNotExist() {
        assertThatThrownBy(() -> filmStorage.findById(999))
                .isInstanceOf(NotFoundException.class);
    }

    private Film createFilm(String name, int mpaId, Integer... genreIds) {
        Film film = new Film();
        film.setName(name);
        film.setDescription("Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        film.setMpa(new Mpa(mpaId, null));
        LinkedHashSet<Genre> genres = new LinkedHashSet<>();
        for (Integer genreId : genreIds) {
            genres.add(new Genre(genreId, null));
        }
        film.setGenres(genres);
        return filmStorage.create(film);
    }

    private long createUser(String email, String login) {
        jdbcTemplate.update("INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)",
                email, login, login, LocalDate.of(2000, 1, 1));
        return jdbcTemplate.queryForObject("SELECT user_id FROM users WHERE login = ?", Long.class, login);
    }
}
