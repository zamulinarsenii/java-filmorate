package ru.yandex.practicum.filmorate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FilmController.class)
class FilmControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Film validFilm;

    @BeforeEach
    void setUp() {
        validFilm = new Film();
        validFilm.setName("Valid Film");
        validFilm.setDescription("Good description");
        validFilm.setReleaseDate(LocalDate.of(2000, 1, 1));
        validFilm.setDuration(120);
    }

    // --------------------- POST /films ---------------------

    @Test
    void createFilm_ShouldReturnFilm_WhenValid() throws Exception {
        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validFilm)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Valid Film"))
                .andExpect(jsonPath("$.description").value("Good description"))
                .andExpect(jsonPath("$.releaseDate").value("2000-01-01"))
                .andExpect(jsonPath("$.duration").value(120));
    }

    @Test
    void createFilm_ShouldThrow_WhenNameIsNull() throws Exception {
        validFilm.setName(null);
        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validFilm)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createFilm_ShouldThrow_WhenNameIsBlank() throws Exception {
        validFilm.setName("");
        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validFilm)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createFilm_ShouldThrow_WhenDescriptionTooLong() throws Exception {
        validFilm.setDescription("a".repeat(201));
        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validFilm)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createFilm_ShouldThrow_WhenReleaseDateBeforeMin() throws Exception {
        validFilm.setReleaseDate(LocalDate.of(1895, 12, 27));
        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validFilm)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createFilm_ShouldThrow_WhenDurationIsZero() throws Exception {
        validFilm.setDuration(0);
        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validFilm)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createFilm_ShouldThrow_WhenDurationNegative() throws Exception {
        validFilm.setDuration(-10);
        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validFilm)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createFilm_ShouldThrow_WhenIdProvided() throws Exception {
        validFilm.setId(999L);
        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validFilm)))
                .andExpect(status().isBadRequest());
    }

    // --------------------- PUT /films ---------------------

    @Test
    void updateFilm_ShouldReturnUpdatedFilm_WhenValid() throws Exception {
        // создаём
        String createResp = mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validFilm)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Film created = objectMapper.readValue(createResp, Film.class);

        Film update = new Film();
        update.setId(created.getId());
        update.setName("New Name");
        update.setDescription("New description");
        update.setReleaseDate(LocalDate.of(2020, 5, 5));
        update.setDuration(150);

        mockMvc.perform(put("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.description").value("New description"))
                .andExpect(jsonPath("$.releaseDate").value("2020-05-05"))
                .andExpect(jsonPath("$.duration").value(150));
    }

    @Test
    void updateFilm_ShouldThrow_WhenIdIsNull() throws Exception {
        Film update = new Film();
        update.setName("Name");
        mockMvc.perform(put("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateFilm_ShouldThrow_WhenFilmNotFound() throws Exception {
        Film update = new Film();
        update.setId(999L);
        update.setName("Name");
        mockMvc.perform(put("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateFilm_ShouldThrow_WhenNameIsBlankInUpdate() throws Exception {
        String createResp = mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validFilm)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Film created = objectMapper.readValue(createResp, Film.class);

        Film update = new Film();
        update.setId(created.getId());
        update.setName("");   // blank name should cause validation exception

        mockMvc.perform(put("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateFilm_ShouldThrow_WhenDescriptionTooLong() throws Exception {
        String createResp = mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validFilm)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Film created = objectMapper.readValue(createResp, Film.class);

        Film update = new Film();
        update.setId(created.getId());
        update.setDescription("a".repeat(201));

        mockMvc.perform(put("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isBadRequest());
    }
}