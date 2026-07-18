package ru.yandex.practicum.filmorate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@Transactional
class FilmorateApplicationIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldExposeReferenceEndpoints() throws Exception {
        mockMvc.perform(get("/genres"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Комедия"));

        mockMvc.perform(get("/mpa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("G"));

        mockMvc.perform(get("/mpa/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldPersistFilmWithMpaAndGenresThroughApi() throws Exception {
        String filmJson = """
                {
                  "name": "Database Film",
                  "description": "Stored in H2",
                  "releaseDate": "2000-01-01",
                  "duration": 120,
                  "mpa": {"id": 1},
                  "genres": [{"id": 1}, {"id": 2}]
                }
                """;

        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(filmJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.mpa.name").value("G"))
                .andExpect(jsonPath("$.genres.length()").value(2))
                .andExpect(jsonPath("$.genres[0].name").value("Комедия"));
    }

    @Test
    void shouldKeepFriendshipOneWay() throws Exception {
        long firstId = createUser("first@test.ru", "first");
        long secondId = createUser("second@test.ru", "second");

        mockMvc.perform(put("/users/{id}/friends/{friendId}", firstId, secondId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/users/{id}/friends", firstId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(secondId));

        mockMvc.perform(get("/users/{id}/friends", secondId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private long createUser(String email, String login) throws Exception {
        String json = """
                {
                  "email": "%s",
                  "login": "%s",
                  "name": "%s",
                  "birthday": "2000-01-01"
                }
                """.formatted(email, login, login);
        String response = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode user = objectMapper.readTree(response);
        return user.get("id").asLong();
    }
}
