package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilmService {
    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;

    public List<Film> findAll() {
        return filmStorage.findAll();
    }

    public Film findById(long id) {
        return filmStorage.findById(id);
    }

    public Film create(Film film) {
        log.info("Создание фильма: {}", film);
        validateFilmForCreate(film);
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

        filmStorage.update(oldFilm);
        log.info("Фильм обновлён: {}", oldFilm);
        return oldFilm;
    }

    public void addLike(long filmId, long userId) {
        log.info("Добавление лайка: filmId={}, userId={}", filmId, userId);
        Film film = filmStorage.findById(filmId);
        userStorage.findById(userId);

        if (film.getLikes().contains(userId)) {
            log.debug("Пользователь {} уже поставил лайк фильму {}", userId, filmId);
            return;
        }

        film.getLikes().add(userId);
        filmStorage.update(film);
        log.info("Лайк добавлен: filmId={}, userId={}", filmId, userId);
    }

    public void removeLike(long filmId, long userId) {
        log.info("Удаление лайка: filmId={}, userId={}", filmId, userId);
        Film film = filmStorage.findById(filmId);
        userStorage.findById(userId);

        if (!film.getLikes().contains(userId)) {
            log.debug("Пользователь {} не ставил лайк фильму {}", userId, filmId);
            throw new NotFoundException("Пользователь с id " + userId + " не ставил лайк этому фильму");
        }

        film.getLikes().remove(userId);
        filmStorage.update(film);
        log.info("Лайк удалён: filmId={}, userId={}", filmId, userId);
    }

    public List<Film> getPopularFilms(Integer count) {
        if (count == null || count <= 0) {
            count = 10;
        }
        return filmStorage.findAll().stream()
                .sorted(Comparator.comparingInt(f -> -f.getLikes().size()))
                .limit(count)
                .collect(Collectors.toList());
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
}