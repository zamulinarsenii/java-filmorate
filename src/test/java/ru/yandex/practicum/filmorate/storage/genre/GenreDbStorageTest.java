package ru.yandex.practicum.filmorate.storage.genre;

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
@Import(GenreDbStorage.class)
class GenreDbStorageTest {
    @Autowired
    private GenreDbStorage genreStorage;

    @Test
    void shouldReturnGenres() {
        assertThat(genreStorage.findAll()).hasSize(6);
        assertThat(genreStorage.findById(1).getName()).isEqualTo("Комедия");
    }

    @Test
    void shouldThrowForUnknownGenre() {
        assertThatThrownBy(() -> genreStorage.findById(999))
                .isInstanceOf(NotFoundException.class);
    }
}
