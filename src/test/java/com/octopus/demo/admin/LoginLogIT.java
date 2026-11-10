package com.octopus.demo.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.octopus.demo.common.util.PasswordUtils;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Login audit log: success/failure/disabled rows, IP resolution, paged queries")
class LoginLogIT {

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

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM sys_login_log");
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id IN "
                + "(SELECT id FROM sys_user WHERE username LIKE 'e2e_%')");
        jdbcTemplate.update("DELETE FROM sys_user WHERE username LIKE 'e2e_%'");
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    private String login(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("username", username, "password", password));
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(response).path("data").path("token").asText();
    }

    private Map<String, Object> latestLog() {
        return jdbcTemplate.queryForMap("SELECT * FROM sys_login_log ORDER BY id DESC LIMIT 1");
    }

    private long insertLog(String username, int status, String message, LocalDateTime loginAt) {
        jdbcTemplate.update(
                "INSERT INTO sys_login_log (username, ip, user_agent, status, message, login_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                username, "10.0.0.1", "it-agent", status, message, loginAt);
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM sys_login_log", Long.class);
    }

    @Test
    @DisplayName("successful login writes status=1 with username, first X-Forwarded-For address and UA")
    void successIsLoggedWithForwardedIp() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", "admin", "password", "123456"));
        mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", "1.2.3.4, 5.6.7.8")
                        .header(HttpHeaders.USER_AGENT, "JUnit-UA")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        Map<String, Object> row = latestLog();
        assertThat(row.get("username")).isEqualTo("admin");
        assertThat(((Number) row.get("status")).intValue()).isEqualTo(1);
        assertThat(row.get("ip")).isEqualTo("1.2.3.4");
        assertThat(row.get("user_agent")).isEqualTo("JUnit-UA");
        assertThat(row.get("login_at")).isNotNull();
    }

    @Test
    @DisplayName("X-Real-IP is used when X-Forwarded-For is absent; remote address is the last fallback")
    void ipFallbacks() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", "admin", "password", "123456"));

        mockMvc.perform(post("/api/auth/login")
                        .header("X-Real-IP", "9.9.9.9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
        assertThat(latestLog().get("ip")).isEqualTo("9.9.9.9");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
        assertThat(latestLog().get("ip")).isEqualTo("127.0.0.1");
    }

    @Test
    @DisplayName("wrong password writes status=0 with 用户名或密码错误")
    void wrongPasswordIsLogged() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", "admin", "password", "wrong"));
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("X-Forwarded-For", "1.2.3.4"))
                .andExpect(status().isUnauthorized());

        Map<String, Object> row = latestLog();
        assertThat(row.get("username")).isEqualTo("admin");
        assertThat(((Number) row.get("status")).intValue()).isZero();
        assertThat(row.get("message")).isEqualTo("用户名或密码错误");
    }

    @Test
    @DisplayName("disabled account writes status=0 with 账号已停用 and is rejected")
    void disabledAccountIsLogged() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO sys_user (username, password_hash, nickname, status) VALUES (?, ?, ?, 0)",
                "e2e_login_user", PasswordUtils.encode("123456"), "disabled");

        String body = objectMapper.writeValueAsString(
                Map.of("username", "e2e_login_user", "password", "123456"));
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.msg").value("账号已停用"));

        Map<String, Object> row = latestLog();
        assertThat(row.get("username")).isEqualTo("e2e_login_user");
        assertThat(((Number) row.get("status")).intValue()).isZero();
        assertThat((String) row.get("message")).contains("停用");
    }

    @Test
    @DisplayName("query filters by username/status and paginates newest first")
    void queryFiltersAndPaginates() throws Exception {
        String token = login("admin", "123456");
        jdbcTemplate.update("DELETE FROM sys_login_log");

        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 10, 0, 0);
        insertLog("admin", 1, "登录成功", base);
        insertLog("admin", 0, "用户名或密码错误", base.plusMinutes(1));
        insertLog("guest", 1, "登录成功", base.plusMinutes(2));

        mockMvc.perform(get("/api/logs/login")
                        .param("username", "admin")
                        .param("status", "1")
                        .param("page", "1")
                        .param("size", "10")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.count").value(1))
                .andExpect(jsonPath("$.data.list.length()").value(1))
                .andExpect(jsonPath("$.data.list[0].username").value("admin"))
                .andExpect(jsonPath("$.data.list[0].status").value(1));

        mockMvc.perform(get("/api/logs/login")
                        .param("page", "1")
                        .param("size", "2")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(3))
                .andExpect(jsonPath("$.data.list.length()").value(2))
                .andExpect(jsonPath("$.data.list[0].username").value("guest"))
                .andExpect(jsonPath("$.data.list[1].username").value("admin"))
                .andExpect(jsonPath("$.data.list[1].status").value(0));
    }

    @Test
    @DisplayName("query applies the from/to time range")
    void queryFiltersByTimeRange() throws Exception {
        String token = login("admin", "123456");
        jdbcTemplate.update("DELETE FROM sys_login_log");

        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 10, 0, 0);
        insertLog("admin", 1, "old", base);
        insertLog("admin", 1, "recent", base.plusDays(5));

        String from = base.plusDays(2).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        mockMvc.perform(get("/api/logs/login")
                        .param("from", from)
                        .param("page", "1")
                        .param("size", "10")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(1))
                .andExpect(jsonPath("$.data.list[0].message").value("recent"));
    }

    @Test
    @DisplayName("guest cannot read login logs")
    void guestIsForbidden() throws Exception {
        String token = login("guest", "123456");
        mockMvc.perform(get("/api/logs/login")
                        .param("page", "1")
                        .param("size", "10")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @DisplayName("anonymous cannot read login logs")
    void anonymousIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/logs/login")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isUnauthorized());
    }
}
