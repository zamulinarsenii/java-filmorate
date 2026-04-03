package ru.yandex.practicum.filmorate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({UserService.class, InMemoryUserStorage.class})
class UserControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InMemoryUserStorage userStorage; // для очистки

    @MockBean
    private FilmStorage filmStorage; // FilmService не используется, но нужен для контекста

    private User validUser;

    @BeforeEach
    void setUp() {
        // очищаем хранилище перед каждым тестом
        userStorage.clear(); // метод нужно добавить в InMemoryUserStorage

        validUser = new User();
        validUser.setEmail("user@example.com");
        validUser.setLogin("login123");
        validUser.setName("Name");
        validUser.setBirthday(LocalDate.of(2000, 1, 1));
    }

    // --------------------- POST /users ---------------------

    @Test
    void createUser_ShouldReturnUser_WhenValid() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.login").value("login123"))
                .andExpect(jsonPath("$.name").value("Name"))
                .andExpect(jsonPath("$.birthday").value("2000-01-01"));
    }

    @Test
    void createUser_ShouldSetNameFromLogin_WhenNameIsBlank() throws Exception {
        User userWithBlankName = new User();
        userWithBlankName.setEmail("test@test.com");
        userWithBlankName.setLogin("testLogin");
        userWithBlankName.setName("");
        userWithBlankName.setBirthday(LocalDate.now().minusYears(1));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userWithBlankName)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("testLogin"));
    }

    @Test
    void createUser_ShouldThrow_WhenEmailIsNull() throws Exception {
        User invalid = new User();
        invalid.setEmail(null);
        invalid.setLogin("login");
        invalid.setName("Name");
        invalid.setBirthday(LocalDate.now().minusYears(1));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_ShouldThrow_WhenEmailIsBlank() throws Exception {
        User invalid = new User();
        invalid.setEmail("");
        invalid.setLogin("login");
        invalid.setName("Name");
        invalid.setBirthday(LocalDate.now().minusYears(1));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_ShouldThrow_WhenEmailMissingAtSymbol() throws Exception {
        User invalid = new User();
        invalid.setEmail("user.example.com");
        invalid.setLogin("login");
        invalid.setName("Name");
        invalid.setBirthday(LocalDate.now().minusYears(1));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_ShouldThrow_WhenLoginIsNull() throws Exception {
        User invalid = new User();
        invalid.setEmail("user@example.com");
        invalid.setLogin(null);
        invalid.setName("Name");
        invalid.setBirthday(LocalDate.now().minusYears(1));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_ShouldThrow_WhenLoginIsBlank() throws Exception {
        User invalid = new User();
        invalid.setEmail("user@example.com");
        invalid.setLogin("");
        invalid.setName("Name");
        invalid.setBirthday(LocalDate.now().minusYears(1));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_ShouldThrow_WhenLoginContainsSpaces() throws Exception {
        User invalid = new User();
        invalid.setEmail("user@example.com");
        invalid.setLogin("my login");
        invalid.setName("Name");
        invalid.setBirthday(LocalDate.now().minusYears(1));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_ShouldThrow_WhenBirthdayInFuture() throws Exception {
        User invalid = new User();
        invalid.setEmail("user@example.com");
        invalid.setLogin("login");
        invalid.setName("Name");
        invalid.setBirthday(LocalDate.now().plusDays(1));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_ShouldThrow_WhenIdIsProvided() throws Exception {
        User invalid = new User();
        invalid.setId(999L);
        invalid.setEmail("user@example.com");
        invalid.setLogin("login");
        invalid.setName("Name");
        invalid.setBirthday(LocalDate.now().minusYears(1));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    // --------------------- PUT /users ---------------------

    @Test
    void updateUser_ShouldReturnUpdatedUser_WhenValid() throws Exception {
        // сначала создаём пользователя
        String createResp = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUser)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        User created = objectMapper.readValue(createResp, User.class);

        User update = new User();
        update.setId(created.getId());
        update.setEmail("newemail@test.com");
        update.setLogin("newLogin");
        update.setName("NewName");
        update.setBirthday(LocalDate.of(1999, 12, 31));

        mockMvc.perform(put("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("newemail@test.com"))
                .andExpect(jsonPath("$.login").value("newLogin"))
                .andExpect(jsonPath("$.name").value("NewName"))
                .andExpect(jsonPath("$.birthday").value("1999-12-31"));
    }

    @Test
    void updateUser_ShouldThrow_WhenUserNotFound() throws Exception {
        User update = new User();
        update.setId(999L);
        update.setEmail("email@test.com");
        update.setLogin("login");
        update.setName("Name");
        update.setBirthday(LocalDate.now().minusYears(1));

        mockMvc.perform(put("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateUser_ShouldThrow_WhenEmailInvalid() throws Exception {
        String createResp = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUser)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        User created = objectMapper.readValue(createResp, User.class);

        User invalid = new User();
        invalid.setId(created.getId());
        invalid.setEmail("noAtSymbol");
        invalid.setLogin(created.getLogin());
        invalid.setName(created.getName());
        invalid.setBirthday(created.getBirthday());

        mockMvc.perform(put("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }
}