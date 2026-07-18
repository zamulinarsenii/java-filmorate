package ru.yandex.practicum.filmorate.storage.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JdbcTest
@AutoConfigureTestDatabase
@Import(UserDbStorage.class)
class UserDbStorageTest {
    @Autowired
    private UserDbStorage userStorage;

    @Test
    void shouldCreateFindUpdateAndListUsers() {
        User user = createUser("one@test.ru", "one");

        assertThat(user.getId()).isPositive();
        assertThat(userStorage.findById(user.getId()).getLogin()).isEqualTo("one");

        user.setName("Updated");
        userStorage.update(user);

        assertThat(userStorage.findById(user.getId()).getName()).isEqualTo("Updated");
        assertThat(userStorage.findAll()).extracting(User::getId).contains(user.getId());
    }

    @Test
    void shouldManageOneWayFriendshipsAndFindCommonFriends() {
        User first = createUser("first@test.ru", "first");
        User second = createUser("second@test.ru", "second");
        User common = createUser("common@test.ru", "common");

        userStorage.addFriend(first.getId(), second.getId());
        userStorage.addFriend(first.getId(), common.getId());
        userStorage.addFriend(second.getId(), common.getId());

        assertThat(userStorage.getFriends(first.getId()))
                .extracting(User::getId)
                .containsExactly(second.getId(), common.getId());
        assertThat(userStorage.getFriends(second.getId()))
                .extracting(User::getId)
                .containsExactly(common.getId());
        assertThat(userStorage.getCommonFriends(first.getId(), second.getId()))
                .extracting(User::getId)
                .containsExactly(common.getId());

        userStorage.removeFriend(first.getId(), second.getId());
        assertThat(userStorage.getFriends(first.getId()))
                .extracting(User::getId)
                .containsExactly(common.getId());
    }

    @Test
    void shouldThrowWhenUserDoesNotExist() {
        assertThatThrownBy(() -> userStorage.findById(999))
                .isInstanceOf(NotFoundException.class);
    }

    private User createUser(String email, String login) {
        return userStorage.create(new User(null, email, login, login, LocalDate.of(2000, 1, 1)));
    }
}
