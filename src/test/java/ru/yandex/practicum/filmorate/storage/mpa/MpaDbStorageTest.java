package ru.yandex.practicum.filmorate.storage.mpa;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.exception.NotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JdbcTest
@AutoConfigureTestDatabase
@Import(MpaDbStorage.class)
class MpaDbStorageTest {
    @Autowired
    private MpaDbStorage mpaStorage;

    @Test
    void shouldReturnMpaRatings() {
        assertThat(mpaStorage.findAll()).hasSize(5);
        assertThat(mpaStorage.findById(1).getName()).isEqualTo("G");
    }

    @Test
    void shouldThrowForUnknownMpa() {
        assertThatThrownBy(() -> mpaStorage.findById(999))
                .isInstanceOf(NotFoundException.class);
    }
}
