package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserStorage userStorage;

    public List<User> findAll() {
        return userStorage.findAll();
    }

    public User findById(long id) {
        return userStorage.findById(id);
    }

    public User create(User user) {
        log.info("Создание пользователя: {}", user);
        validateUserForCreate(user);
        User created = userStorage.create(user);
        if (created.getName() == null || created.getName().isBlank()) {
            created.setName(created.getLogin());
            userStorage.update(created);
        }
        log.info("Пользователь создан: {}", created);
        return created;
    }

    public User update(User newUser) {
        log.info("Обновление пользователя: {}", newUser);
        if (newUser.getId() == null) {
            throw new ValidationException("Id пользователя должен быть указан");
        }
        User oldUser = userStorage.findById(newUser.getId());

        validateUserForUpdate(newUser);

        // Обновляем только переданные поля
        if (newUser.getEmail() != null && !newUser.getEmail().isBlank()) {
            oldUser.setEmail(newUser.getEmail());
        }
        if (newUser.getLogin() != null && !newUser.getLogin().isBlank()) {
            oldUser.setLogin(newUser.getLogin());
        }
        if (newUser.getName() != null) {
            oldUser.setName(newUser.getName().isBlank() ? oldUser.getLogin() : newUser.getName());
        }
        if (newUser.getBirthday() != null) {
            oldUser.setBirthday(newUser.getBirthday());
        }

        userStorage.update(oldUser);
        log.info("Пользователь обновлён: {}", oldUser);
        return oldUser;
    }

    public void addFriend(long userId, long friendId) {
        log.info("Добавление друга: userId={}, friendId={}", userId, friendId);
        User user = userStorage.findById(userId);
        User friend = userStorage.findById(friendId);

        if (user.getFriends().contains(friendId)) {
            log.debug("Пользователь {} уже является другом {}", userId, friendId);
            return; // уже друзья, ничего не делаем
        }

        user.getFriends().add(friendId);
        friend.getFriends().add(userId);

        userStorage.update(user);
        userStorage.update(friend);
        log.info("Дружба между {} и {} установлена", userId, friendId);
    }

    public void removeFriend(long userId, long friendId) {
        if (userId == friendId) {
            log.warn("Попытка удалить самого себя из друзей");
            throw new ValidationException("Нельзя удалить самого себя из друзей");
        }

        log.info("Удаление друга: userId={}, friendId={}", userId, friendId);
        User user = userStorage.findById(userId);

        if (!user.getFriends().contains(friendId)) {
            log.debug("Пользователь {} не является другом {}", userId, friendId);
            throw new NotFoundException("Пользователь с id " + friendId + " не является другом");
        }

        User friend = userStorage.findById(friendId);

        user.getFriends().remove(friendId);
        friend.getFriends().remove(userId);

        userStorage.update(user);
        userStorage.update(friend);
        log.info("Дружба между {} и {} удалена", userId, friendId);
    }

    public List<User> getFriends(long userId) {
        User user = userStorage.findById(userId);
        if (user.getFriends().isEmpty()) {
            throw new NotFoundException("У пользователя с id " + userId + " нет друзей");
        }
        return user.getFriends().stream()
                .map(userStorage::findById)
                .collect(Collectors.toList());
    }

    public List<User> getCommonFriends(long userId, long otherId) {
        User user = userStorage.findById(userId);
        User other = userStorage.findById(otherId);

        Set<Long> commonIds = user.getFriends().stream()
                .filter(other.getFriends()::contains)
                .collect(Collectors.toSet());

        return commonIds.stream()
                .map(userStorage::findById)
                .collect(Collectors.toList());
    }

    private void validateUser(User user) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new ValidationException("Электронная почта не может быть пустой");
        }
        if (!user.getEmail().contains("@")) {
            throw new ValidationException("Электронная почта должна содержать символ @");
        }
        if (user.getLogin() == null || user.getLogin().isBlank()) {
            throw new ValidationException("Логин не может быть пустым");
        }
        if (user.getLogin().contains(" ")) {
            throw new ValidationException("Логин не может содержать пробелы");
        }
        if (user.getBirthday() != null && user.getBirthday().isAfter(LocalDate.now())) {
            throw new ValidationException("Дата рождения не может быть в будущем");
        }
    }

    private void validateUserForCreate(User user) {
        if (user.getId() != null) {
            throw new ValidationException("Id присваивается автоматически, не указывайте его");
        }
        validateUser(user);
    }

    private void validateUserForUpdate(User user) {
        validateUser(user);
    }
}