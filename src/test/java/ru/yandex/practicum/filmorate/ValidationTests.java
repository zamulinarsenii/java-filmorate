package ru.yandex.practicum.filmorate;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.controller.UserController;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({FilmController.class, UserController.class})
public class ValidationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	private Film validFilm;
	private User validUser;

	@BeforeEach
	void setUp() {
		validFilm = new Film();
		validFilm.setName("Valid Film");
		validFilm.setDescription("Valid description");
		validFilm.setReleaseDate(LocalDate.of(2000, 1, 1));
		validFilm.setDuration(120);

		validUser = new User();
		validUser.setEmail("user@example.com");
		validUser.setLogin("validLogin");
		validUser.setName("Valid Name");
		validUser.setBirthday(LocalDate.of(1990, 1, 1));
	}

	// ==================== FILM TESTS ====================

	@Test
	void createFilm_WithValidData_ShouldReturnOk() throws Exception {
		mockMvc.perform(post("/films")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(validFilm)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").exists())
				.andExpect(jsonPath("$.name").value("Valid Film"));
	}

	@Test
	void createFilm_WithEmptyName_ShouldReturnBadRequest() throws Exception {
		validFilm.setName("");
		mockMvc.perform(post("/films")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(validFilm)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Название фильма не может быть пустым"));
	}

	@Test
	void createFilm_WithNullName_ShouldReturnBadRequest() throws Exception {
		validFilm.setName(null);
		mockMvc.perform(post("/films")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(validFilm)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Название фильма не может быть пустым"));
	}

	@Test
	void createFilm_WithDescriptionTooLong_ShouldReturnBadRequest() throws Exception {
		validFilm.setDescription("a".repeat(201));
		mockMvc.perform(post("/films")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(validFilm)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Описание фильма не может быть длиннее 200 символов"));
	}

	@Test
	void createFilm_WithReleaseDateBeforeMinDate_ShouldReturnBadRequest() throws Exception {
		validFilm.setReleaseDate(LocalDate.of(1895, 12, 27));
		mockMvc.perform(post("/films")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(validFilm)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Дата релиза не может быть раньше 28 декабря 1895 года"));
	}

	@Test
	void createFilm_WithZeroDuration_ShouldReturnBadRequest() throws Exception {
		validFilm.setDuration(0);
		mockMvc.perform(post("/films")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(validFilm)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Продолжительность фильма должна быть положительным числом"));
	}

	@Test
	void createFilm_WithNegativeDuration_ShouldReturnBadRequest() throws Exception {
		validFilm.setDuration(-10);
		mockMvc.perform(post("/films")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(validFilm)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Продолжительность фильма должна быть положительным числом"));
	}

	@Test
	void updateFilm_WithValidData_ShouldReturnOk() throws Exception {
		// сначала создаём фильм
		String response = mockMvc.perform(post("/films")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(validFilm)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		Film created = objectMapper.readValue(response, Film.class);
		created.setName("Updated Name");

		mockMvc.perform(put("/films")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(created)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Updated Name"));
	}

	@Test
	void updateFilm_WithoutId_ShouldReturnBadRequest() throws Exception {
		validFilm.setId(null);
		mockMvc.perform(put("/films")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(validFilm)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Id фильма должен быть указан"));
	}

	@Test
	void updateFilm_WithInvalidField_ShouldReturnBadRequest() throws Exception {
		// создаём фильм
		String response = mockMvc.perform(post("/films")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(validFilm)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		Film created = objectMapper.readValue(response, Film.class);
		created.setDuration(0); // невалидное значение

		mockMvc.perform(put("/films")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(created)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Продолжительность фильма должна быть положительным числом"));
	}

	// ==================== USER TESTS ====================

	@Test
	void createUser_WithValidData_ShouldReturnOk() throws Exception {
		mockMvc.perform(post("/users")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(validUser)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").exists())
				.andExpect(jsonPath("$.email").value("user@example.com"))
				.andExpect(jsonPath("$.login").value("validLogin"))
				.andExpect(jsonPath("$.name").value("Valid Name"));
	}

	@Test
	void createUser_WithEmptyEmail_ShouldReturnBadRequest() throws Exception {
		validUser.setEmail("");
		mockMvc.perform(post("/users")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(validUser)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Электронная почта не может быть пустой"));
	}

	@Test
	void createUser_WithNullEmail_ShouldReturnBadRequest() throws Exception {
		validUser.setEmail(null);
		mockMvc.perform(post("/users")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(validUser)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Электронная почта не может быть пустой"));
	}

	@Test
	void createUser_WithEmailMissingAtSymbol_ShouldReturnBadRequest() throws Exception {
		validUser.setEmail("userexample.com");
		mockMvc.perform(post("/users")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(validUser)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Электронная почта должна содержать символ @"));
	}

	@Test
	void createUser_WithEmptyLogin_ShouldReturnBadRequest() throws Exception {
		validUser.setLogin("");
		mockMvc.perform(post("/users")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(validUser)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Логин не может быть пустым"));
	}

	@Test
	void createUser_WithLoginContainingSpace_ShouldReturnBadRequest() throws Exception {
		validUser.setLogin("login with space");
		mockMvc.perform(post("/users")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(validUser)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Логин не может содержать пробелы"));
	}

	@Test
	void createUser_WithEmptyName_ShouldSetNameToLogin() throws Exception {
		validUser.setName("");
		mockMvc.perform(post("/users")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(validUser)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value(validUser.getLogin()));
	}

	@Test
	void createUser_WithNullName_ShouldSetNameToLogin() throws Exception {
		validUser.setName(null);
		mockMvc.perform(post("/users")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(validUser)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value(validUser.getLogin()));
	}

	@Test
	void createUser_WithBirthdayInFuture_ShouldReturnBadRequest() throws Exception {
		validUser.setBirthday(LocalDate.now().plusDays(1));
		mockMvc.perform(post("/users")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(validUser)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Дата рождения не может быть в будущем"));
	}

	@Test
	void updateUser_WithValidData_ShouldReturnOk() throws Exception {
		// создаём пользователя
		String response = mockMvc.perform(post("/users")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(validUser)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		User created = objectMapper.readValue(response, User.class);
		created.setEmail("new@example.com");

		mockMvc.perform(put("/users")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(created)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("new@example.com"));
	}

	@Test
	void updateUser_WithoutId_ShouldReturnBadRequest() throws Exception {
		validUser.setId(null);
		mockMvc.perform(put("/users")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(validUser)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Id пользователя должен быть указан"));
	}

	@Test
	void updateUser_WithInvalidEmail_ShouldReturnBadRequest() throws Exception {
		String response = mockMvc.perform(post("/users")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(validUser)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		User created = objectMapper.readValue(response, User.class);
		created.setEmail("invalid");
		mockMvc.perform(put("/users")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(created)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Электронная почта должна содержать символ @"));
	}
}