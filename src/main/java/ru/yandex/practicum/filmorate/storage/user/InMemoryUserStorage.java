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

    public void clear() {
        users.clear();
        nextId = 1;
    }
}
