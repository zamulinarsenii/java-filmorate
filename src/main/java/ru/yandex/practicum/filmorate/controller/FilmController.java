package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/films")
public class FilmController {

    private final Map<Long, Film> films = new HashMap<>();
    private long nextId = 1;

    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    @GetMapping
    public List<Film> findAll() {
        log.debug("Текущее количество фильмов: {}", films.size());
        return List.copyOf(films.values());
    }

    @PostMapping
    public Film create(@RequestBody Film film) {
        log.info("Запрос на создание фильма: {}", film);
        validateFilmForCreate(film);
        film.setId(nextId++);
        films.put(film.getId(), film);
        log.info("Фильм создан: {}", film);
        return film;
    }

    @PutMapping
    public Film update(@RequestBody Film newFilm) {
        log.info("Запрос на обновление фильма: {}", newFilm);
        if (newFilm.getId() == null) {
            log.warn("Попытка обновления фильма без ID");
            throw new ValidationException("Id фильма должен быть указан");
        }
        Film oldFilm = films.get(newFilm.getId());
        if (oldFilm == null) {
            log.warn("Фильм с id {} не найден", newFilm.getId());
            throw new ValidationException("Фильм с id " + newFilm.getId() + " не найден");
        }

        // Валидация только переданных полей (не пустых)
        validateFilmForUpdate(newFilm);

        // Обновляем только непустые поля
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

        log.info("Фильм обновлён: {}", oldFilm);
        return oldFilm;
    }
    private void validateFilm(Film film) {
        if (film.getDescription() != null && film.getDescription().length() > 200) {
            log.warn("Описание фильма превышает 200 символов: {}", film.getDescription().length());
            throw new ValidationException("Описание фильма не может быть длиннее 200 символов");
        }
        if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(MIN_RELEASE_DATE)) {
            log.warn("Дата релиза {} раньше 28 декабря 1895 года", film.getReleaseDate());
            throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года");
        }
        if (film.getDuration() <= 0) {
            log.warn("Продолжительность фильма не положительная: {}", film.getDuration());
            throw new ValidationException("Продолжительность фильма должна быть положительным числом");
        }
    }
    private void validateFilmForCreate(Film film) {
        if (film.getId() != null) {
            log.warn("Попытка создать фильм с предустановленным id: {}", film.getId());
            throw new ValidationException("Id присваивается автоматически, не указывайте его");
        }
        if (film.getName() == null || film.getName().isBlank()) {
            log.warn("Название фильма пустое");
            throw new ValidationException("Название фильма не может быть пустым");
        }
        validateFilm(film);
    }

    private void validateFilmForUpdate(Film film) {
        if (film.getName() != null && film.getName().isBlank()) {
            log.warn("Попытка установить пустое название при обновлении");
            throw new ValidationException("Название фильма не может быть пустым");
        }
        validateFilm(film);
    }
}