package ru.yandex.practicum.filmorate.storage.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class InMemoryUserStorage implements UserStorage {
    private final Map<Long, User> users = new HashMap<>();
    private long nextId = 1;

    @Override
    public List<User> findAll() {
        log.debug("InMemoryUserStorage: получение всех пользователей, количество: {}", users.size());
        return List.copyOf(users.values());
    }

    @Override
    public User create(User user) {
        user.setId(nextId++);
        users.put(user.getId(), user);
        log.debug("InMemoryUserStorage: создан пользователь с id {}", user.getId());
        return user;
    }

    @Override
    public User update(User user) {
        users.put(user.getId(), user);
        log.debug("InMemoryUserStorage: обновлён пользователь с id {}", user.getId());
        return user;
    }

    @Override
    public User findById(long id) {
        User user = users.get(id);
        if (user == null) {
            log.warn("InMemoryUserStorage: пользователь с id {} не найден", id);
            throw new NotFoundException("Пользователь с id " + id + " не найден");
        }
        return user;
    }

    @Override
    public void addFriend(long userId, long friendId) {
        findById(userId).getFriends().add(friendId);
    }

    @Override
    public void removeFriend(long userId, long friendId) {
        findById(userId).getFriends().remove(friendId);
    }

    @Override
    public List<User> getFriends(long userId) {
        return findById(userId).getFriends().stream()
                .map(this::findById)
                .toList();
    }

    @Override
    public List<User> getCommonFriends(long userId, long otherId) {
        User other = findById(otherId);
        return findById(userId).getFriends().stream()
                .filter(other.getFriends()::contains)
                .map(this::findById)
                .toList();
    }

    public void clear() {
        users.clear();
        nextId = 1;
    }
}
