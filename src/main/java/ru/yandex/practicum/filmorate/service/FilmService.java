package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreStorage;
import ru.yandex.practicum.filmorate.storage.mpa.MpaStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilmService {
    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final GenreStorage genreStorage;
    private final MpaStorage mpaStorage;

    public List<Film> findAll() {
        return filmStorage.findAll();
    }

    public Film findById(long id) {
        return filmStorage.findById(id);
    }

    public Film create(Film film) {
        log.info("Создание фильма: {}", film);
        validateFilmForCreate(film);
        normalizeReferences(film);
        return filmStorage.create(film);
    }

    public Film update(Film newFilm) {
        log.info("Обновление фильма: {}", newFilm);
        if (newFilm.getId() == null) {
            throw new ValidationException("Id фильма должен быть указан");
        }
        Film oldFilm = filmStorage.findById(newFilm.getId());

        validateFilmForUpdate(newFilm);

        if (newFilm.getName() != null && !newFilm.getName().isBlank()) {
            oldFilm.setName(newFilm.getName());
        }
        if (newFilm.getDescription() != null && !newFilm.getDescription().isBlank()) {
            oldFilm.setDescription(newFilm.getDescription());
        }
        if (newFilm.getReleaseDate() != null) {
            oldFilm.setReleaseDate(newFilm.getReleaseDate());
        }
        if (newFilm.getDuration() > 0) {
            oldFilm.setDuration(newFilm.getDuration());
        }
        if (newFilm.getMpa() != null) {
            oldFilm.setMpa(newFilm.getMpa());
        }
        if (newFilm.getGenres() != null) {
            oldFilm.setGenres(newFilm.getGenres());
        }

        normalizeReferences(oldFilm);
        oldFilm = filmStorage.update(oldFilm);
        log.info("Фильм обновлён: {}", oldFilm);
        return oldFilm;
    }

    public void addLike(long filmId, long userId) {
        log.info("Добавление лайка: filmId={}, userId={}", filmId, userId);
        filmStorage.findById(filmId);
        userStorage.findById(userId);
        filmStorage.addLike(filmId, userId);
        log.info("Лайк добавлен: filmId={}, userId={}", filmId, userId);
    }

    public void removeLike(long filmId, long userId) {
        log.info("Удаление лайка: filmId={}, userId={}", filmId, userId);
        filmStorage.findById(filmId);
        userStorage.findById(userId);

        if (!filmStorage.removeLike(filmId, userId)) {
            log.debug("Пользователь {} не ставил лайк фильму {}", userId, filmId);
            throw new NotFoundException("Пользователь с id " + userId + " не ставил лайк этому фильму");
        }
        log.info("Лайк удалён: filmId={}, userId={}", filmId, userId);
    }

    public List<Film> getPopularFilms(Integer count) {
        if (count == null || count <= 0) {
            count = 10;
        }
        return filmStorage.getPopularFilms(count);
    }

    private void validateFilm(Film film) {
        if (film.getName() == null || film.getName().isBlank()) {
            throw new ValidationException("Название фильма не может быть пустым");
        }
        if (film.getDescription() != null && film.getDescription().length() > 200) {
            throw new ValidationException("Описание фильма не может быть длиннее 200 символов");
        }
        if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(MIN_RELEASE_DATE)) {
            throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года");
        }
        if (film.getDuration() <= 0) {
            throw new ValidationException("Продолжительность фильма должна быть положительным числом");
        }
    }

    private void validateFilmForCreate(Film film) {
        if (film.getId() != null) {
            throw new ValidationException("Id присваивается автоматически, не указывайте его");
        }
        validateFilm(film);
    }

    private void validateFilmForUpdate(Film film) {
        validateFilm(film);
    }

    private void normalizeReferences(Film film) {
        if (film.getMpa() != null) {
            if (film.getMpa().getId() == null) {
                throw new ValidationException("Id рейтинга MPA должен быть указан");
            }
            film.setMpa(mpaStorage.findById(film.getMpa().getId()));
        }

        if (film.getGenres() == null) {
            film.setGenres(new LinkedHashSet<>());
            return;
        }

        LinkedHashSet<Genre> genres = new LinkedHashSet<>();
        for (Genre genre : film.getGenres()) {
            if (genre == null || genre.getId() == null) {
                throw new ValidationException("Id жанра должен быть указан");
            }
            genres.add(genreStorage.findById(genre.getId()));
        }
        film.setGenres(genres);
    }
}
