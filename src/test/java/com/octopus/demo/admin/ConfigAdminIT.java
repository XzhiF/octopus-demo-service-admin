package com.octopus.demo.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Parameter admin CRUD, uniqueness, key lookup and 404")
class ConfigAdminIT {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("octopus_demo_system")
            .withUsername("octopus")
            .withPassword("octopus")
            .withInitScript("db/schema.sql");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        cleanUp();
        token = login("admin");
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM sys_config WHERE config_key LIKE 'e2e%'");
    }

    private String login(String username) throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("username", username, "password", "123456"));
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(response).path("data").path("token").asText();
    }

    private JsonNode performPost(String token, Map<String, Object> payload) throws Exception {
        String response = mockMvc.perform(post("/api/configs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(response);
    }

    private JsonNode performPut(long id, Map<String, Object> payload) throws Exception {
        String response = mockMvc.perform(put("/api/configs/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(response);
    }

    @Test
    @DisplayName("seeded sys.site.title is readable by key")
    void seededSiteTitle() throws Exception {
        mockMvc.perform(get("/api/configs/key/sys.site.title")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("Octopus 管理平台"));
    }

    @Test
    @DisplayName("create persists the value and duplicate key returns 400")
    void createPersistsAndRejectsDuplicate() throws Exception {
        JsonNode created = performPost(token, Map.of(
                "configKey", "e2e_key", "configValue", "e2e_value", "remark", "it"));
        assertThat(created.path("code").asInt()).isEqualTo(200);
        assertThat(created.path("data").path("id").asLong()).isPositive();

        String stored = jdbcTemplate.queryForObject(
                "SELECT config_value FROM sys_config WHERE config_key = 'e2e_key'", String.class);
        assertThat(stored).isEqualTo("e2e_value");

        JsonNode duplicate = performPost(token, Map.of(
                "configKey", "e2e_key", "configValue", "other"));
        assertThat(duplicate.path("code").asInt()).isEqualTo(400);
        Integer rows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_config WHERE config_key = 'e2e_key'", Integer.class);
        assertThat(rows).isEqualTo(1);
    }

    @Test
    @DisplayName("config_key is restricted to [A-Za-z0-9._-]+")
    void rejectsInvalidKey() throws Exception {
        JsonNode bad = performPost(token, Map.of(
                "configKey", "e2e bad key!", "configValue", "x"));
        assertThat(bad.path("code").asInt()).isEqualTo(400);

        JsonNode blank = performPost(token, Map.of(
                "configKey", "  ", "configValue", "x"));
        assertThat(blank.path("code").asInt()).isEqualTo(400);

        Integer rows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_config WHERE config_key LIKE 'e2e%'", Integer.class);
        assertThat(rows).isZero();
    }

    @Test
    @DisplayName("update rewrites the value and key read reflects it")
    void updateThenReadByKey() throws Exception {
        JsonNode created = performPost(token, Map.of(
                "configKey", "e2e_key", "configValue", "e2e_value"));
        long id = created.path("data").path("id").asLong();

        JsonNode updated = performPut(id, Map.of(
                "configKey", "e2e_key", "configValue", "e2e_value2", "remark", "changed"));
        assertThat(updated.path("code").asInt()).isEqualTo(200);

        mockMvc.perform(get("/api/configs/key/e2e_key")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("e2e_value2"));
    }

    @Test
    @DisplayName("delete returns 200 and the key read becomes 404")
    void deleteThenKeyReadIs404() throws Exception {
        JsonNode created = performPost(token, Map.of(
                "configKey", "e2e_key", "configValue", "e2e_value"));
        long id = created.path("data").path("id").asLong();

        mockMvc.perform(delete("/api/configs/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/configs/key/e2e_key")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));

        mockMvc.perform(delete("/api/configs/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("unknown key returns 404")
    void unknownKeyReturns404() throws Exception {
        mockMvc.perform(get("/api/configs/key/not_exists")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("list is paginated and filters by key")
    void listIsPaginatedAndFiltered() throws Exception {
        performPost(token, Map.of("configKey", "e2e_alpha", "configValue", "1"));
        performPost(token, Map.of("configKey", "e2e_beta", "configValue", "2"));

        mockMvc.perform(get("/api/configs")
                        .param("page", "1")
                        .param("size", "10")
                        .param("key", "e2e_")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.count").value(2))
                .andExpect(jsonPath("$.data.list[0].configKey").value("e2e_alpha"))
                .andExpect(jsonPath("$.data.list[1].configKey").value("e2e_beta"));

        mockMvc.perform(get("/api/configs")
                        .param("page", "1")
                        .param("size", "1")
                        .param("key", "e2e_")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(2))
                .andExpect(jsonPath("$.data.list.length()").value(1));
    }

    @Test
    @DisplayName("guest can read a key value but cannot manage configs")
    void guestCanReadButNotWrite() throws Exception {
        String guestToken = login("guest");
        mockMvc.perform(get("/api/configs/key/sys.site.title")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + guestToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/configs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + guestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("configKey", "e2e_guest", "configValue", "x"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }
}
