package com.octopus.demo.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Auth API: real login, current user, logout")
class AuthControllerIT {

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

    private String login(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(
                java.util.Map.of("username", username, "password", password));
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode root = objectMapper.readTree(response);
        assertThat(root.path("code").asInt()).isEqualTo(200);
        String token = root.path("data").path("token").asText();
        assertThat(token).isNotBlank();
        return token;
    }

    @Test
    @DisplayName("admin login returns a JWT plus user and permissions without any password field")
    void loginReturnsTokenAndSanitizedUser() throws Exception {
        String body = """
                {"username":"admin","password":"123456"}
                """;
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.user.username").value("admin"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        JsonNode data = objectMapper.readTree(response).path("data");
        assertThat(data.path("expiresInSeconds").asLong()).isPositive();
        assertThat(data.path("permissions").size()).isPositive();
        assertThat(response).doesNotContain("password");
        assertThat(response).doesNotContain("passwordHash");
    }

    @Test
    @DisplayName("wrong password returns HTTP 401 and code 401 regardless of account existence")
    void wrongPasswordReturnsUnauthorized() throws Exception {
        for (String username : new String[]{"admin", "no-such-user"}) {
            String body = objectMapper.writeValueAsString(
                    java.util.Map.of("username", username, "password", "wrong-password"));
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(401))
                    .andExpect(jsonPath("$.msg").isNotEmpty());
        }
    }

    @Test
    @DisplayName("me returns current user, role codes and permission codes; logout is stateless")
    void meAndLogout() throws Exception {
        String token = login("admin", "123456");

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.user.username").value("admin"))
                .andExpect(jsonPath("$.data.roles[0]").value("admin"))
                .andExpect(jsonPath("$.data.permissions").isNotEmpty());

        mockMvc.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("me without a token is rejected with 401")
    void meRequiresToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }
}
