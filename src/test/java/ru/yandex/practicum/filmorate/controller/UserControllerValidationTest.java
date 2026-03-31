package ru.yandex.practicum.filmorate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private User validUser;

    @BeforeEach
    void setUp() {
        validUser = new User(null, "user@example.com", "login123", "Name", LocalDate.of(2000, 1, 1));
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
        User userWithBlankName = new User(null, "test@test.com", "testLogin", "", LocalDate.now().minusYears(1));
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userWithBlankName)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("testLogin"));
    }

    @Test
    void createUser_ShouldThrow_WhenEmailIsNull() throws Exception {
        User invalid = new User(null, null, "login", "Name", LocalDate.now().minusYears(1));
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_ShouldThrow_WhenEmailIsBlank() throws Exception {
        User invalid = new User(null, "", "login", "Name", LocalDate.now().minusYears(1));
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_ShouldThrow_WhenEmailMissingAtSymbol() throws Exception {
        User invalid = new User(null, "user.example.com", "login", "Name", LocalDate.now().minusYears(1));
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_ShouldThrow_WhenLoginIsNull() throws Exception {
        User invalid = new User(null, "user@example.com", null, "Name", LocalDate.now().minusYears(1));
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_ShouldThrow_WhenLoginIsBlank() throws Exception {
        User invalid = new User(null, "user@example.com", "", "Name", LocalDate.now().minusYears(1));
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_ShouldThrow_WhenLoginContainsSpaces() throws Exception {
        User invalid = new User(null, "user@example.com", "my login", "Name", LocalDate.now().minusYears(1));
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_ShouldThrow_WhenBirthdayInFuture() throws Exception {
        User invalid = new User(null, "user@example.com", "login", "Name", LocalDate.now().plusDays(1));
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_ShouldThrow_WhenIdIsProvided() throws Exception {
        User invalid = new User(999L, "user@example.com", "login", "Name", LocalDate.now().minusYears(1));
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

        User update = new User(created.getId(), "newemail@test.com", "newLogin", "NewName", LocalDate.of(1999, 12, 31));
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
    void updateUser_ShouldThrow_WhenIdIsNull() throws Exception {
        User update = new User(null, "email@test.com", "login", "Name", LocalDate.now().minusYears(1));
        mockMvc.perform(put("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateUser_ShouldThrow_WhenUserNotFound() throws Exception {
        User update = new User(999L, "email@test.com", "login", "Name", LocalDate.now().minusYears(1));
        mockMvc.perform(put("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateUser_ShouldThrow_WhenEmailInvalid() throws Exception {
        String createResp = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUser)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        User created = objectMapper.readValue(createResp, User.class);

        User invalid = new User(created.getId(), "noAtSymbol", created.getLogin(), created.getName(), created.getBirthday());
        mockMvc.perform(put("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }
}