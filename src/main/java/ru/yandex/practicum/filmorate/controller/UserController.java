package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {

    private final Map<Long, User> users = new HashMap<>();
    private long nextId = 1;

    @GetMapping
    public List<User> findAll() {
        log.debug("Текущее количество пользователей: {}", users.size());
        return List.copyOf(users.values());
    }

    @PostMapping
    public User create(@RequestBody User user) {
        log.info("Запрос на создание пользователя: {}", user);
        validateUserForCreate(user);
        user.setId(nextId++);
        users.put(user.getId(), user);
        log.info("Пользователь создан: {}", user);
        return user;
    }

    @PutMapping
    public User update(@RequestBody User newUser) {
        log.info("Запрос на обновление пользователя: {}", newUser);
        if (newUser.getId() == null) {
            log.warn("Попытка обновления пользователя без ID");
            throw new ValidationException("Id пользователя должен быть указан");
        }
        User oldUser = users.get(newUser.getId());
        if (oldUser == null) {
            log.warn("Пользователь с id {} не найден", newUser.getId());
            throw new ValidationException("Пользователь с id " + newUser.getId() + " не найден");
        }

        // Валидация обновляемых полей
        validateUserForUpdate(newUser);

        // Обновляем только те поля, которые переданы и не null
        if (newUser.getEmail() != null && !newUser.getEmail().isBlank()) {
            oldUser.setEmail(newUser.getEmail());
        }
        if (newUser.getLogin() != null && !newUser.getLogin().isBlank()) {
            oldUser.setLogin(newUser.getLogin());
        }
        if (newUser.getName() != null) {
            // Если имя пустое, используем логин (текущий или обновлённый)
            if (newUser.getName().isBlank()) {
                oldUser.setName(oldUser.getLogin());
            } else {
                oldUser.setName(newUser.getName());
            }
        }
        if (newUser.getBirthday() != null) {
            oldUser.setBirthday(newUser.getBirthday());
        }

        log.info("Пользователь обновлён: {}", oldUser);
        return oldUser;
    }

    private void validateUser(User user) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            log.warn("Email пользователя пустой");
            throw new ValidationException("Электронная почта не может быть пустой");
        }
        if (!user.getEmail().contains("@")) {
            log.warn("Email не содержит символ @: {}", user.getEmail());
            throw new ValidationException("Электронная почта должна содержать символ @");
        }
        if (user.getLogin() == null || user.getLogin().isBlank()) {
            log.warn("Логин пользователя пустой");
            throw new ValidationException("Логин не может быть пустым");
        }
        if (user.getLogin().contains(" ")) {
            log.warn("Логин содержит пробелы: {}", user.getLogin());
            throw new ValidationException("Логин не может содержать пробелы");
        }
        if (user.getBirthday() != null && user.getBirthday().isAfter(LocalDate.now())) {
            log.warn("Дата рождения {} в будущем", user.getBirthday());
            throw new ValidationException("Дата рождения не может быть в будущем");
        }
    }

    private void validateUserForCreate(User user) {
        if (user.getId() != null) {
            log.warn("Попытка создать пользователя с предустановленным id: {}", user.getId());
            throw new ValidationException("Id присваивается автоматически, не указывайте его");
        }
        validateUser(user);

    }

    private void validateUserForUpdate(User user) {
        validateUser(user);
    }
}