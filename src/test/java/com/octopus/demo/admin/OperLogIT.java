package com.octopus.demo.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.octopus.demo.admin.annotation.OperLog;
import com.octopus.demo.admin.controller.ConfigController;
import com.octopus.demo.admin.controller.DictController;
import com.octopus.demo.admin.controller.JobController;
import com.octopus.demo.admin.controller.MenuController;
import com.octopus.demo.admin.controller.RoleController;
import com.octopus.demo.admin.controller.UserController;
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

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Operation log: success/failure capture, masking, truncation, IP, query and annotation coverage")
class OperLogIT {

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

    private static final String E2E_PREFIX = "e2e_oper_";

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
        token = login();
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM sys_user WHERE username LIKE ?", E2E_PREFIX + "%");
        jdbcTemplate.update("DELETE FROM sys_oper_log");
    }

    private String login() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", "admin", "password", "123456"));
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(response).path("data").path("token").asText();
    }

    private Map<String, Object> createUser(String username, String password) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("username", username);
        payload.put("password", password);
        payload.put("nickname", "操作日志测试");
        payload.put("status", 1);
        return payload;
    }

    private org.springframework.test.web.servlet.ResultActions postUser(Map<String, Object> payload)
            throws Exception {
        return mockMvc.perform(post("/api/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)));
    }

    private Map<String, Object> latestLog() {
        return jdbcTemplate.queryForMap(
                "SELECT id, title, module, http_method, request_uri, params, `operator`, ip, status, "
                        + "error_msg, duration_ms, created_at FROM sys_oper_log ORDER BY id DESC LIMIT 1");
    }

    @Test
    @DisplayName("a successful write logs operator, path, method, duration and masked params")
    void successWritesMaskedLog() throws Exception {
        String username = E2E_PREFIX + "ok";
        postUser(createUser(username, "123456")).andExpect(status().isOk());

        Map<String, Object> row = latestLog();
        assertEquals("admin", row.get("operator"));
        assertEquals("user", row.get("module"));
        assertEquals("新增用户", row.get("title"));
        assertEquals("POST", row.get("http_method"));
        assertEquals("/api/users", row.get("request_uri"));
        assertEquals(1, ((Number) row.get("status")).intValue());
        assertNotNull(row.get("duration_ms"));
        assertNotNull(row.get("created_at"));

        String params = (String) row.get("params");
        assertNotNull(params);
        assertTrue(params.contains(username), "params should contain the request body");
        assertTrue(params.contains("******"), "password field must be masked");
        assertFalse(params.contains("123456"), "plain-text password must never be stored");
    }

    @Test
    @DisplayName("a failing write logs status=0 with a bounded error message and still rethrows")
    void failureWritesErrorLogAndRethrows() throws Exception {
        String username = E2E_PREFIX + "dup";
        postUser(createUser(username, "123456")).andExpect(status().isOk());

        postUser(createUser(username, "123456"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        Map<String, Object> row = latestLog();
        assertEquals(0, ((Number) row.get("status")).intValue());
        String errorMsg = (String) row.get("error_msg");
        assertNotNull(errorMsg);
        assertFalse(errorMsg.isBlank());
        assertTrue(errorMsg.length() <= 500, "error_msg must be truncated to 500 characters");
        assertTrue(errorMsg.contains("已存在"), "error message should carry the thrown cause");

        String params = (String) row.get("params");
        assertTrue(params != null && params.contains(username));
        assertFalse(params.contains("123456"));
    }

    @Test
    @DisplayName("params are truncated to 2000 characters")
    void paramsTruncatedTo2000() {
        String hugeUsername = E2E_PREFIX + "x".repeat(2500);
        // The username overflows sys_user.username, so the request fails at the DB layer; the
        // exception escapes MockMvc after the aspect has already written the log row.
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class,
                () -> postUser(createUser(hugeUsername, "123456")));

        Map<String, Object> row = latestLog();
        String params = (String) row.get("params");
        assertNotNull(params);
        assertEquals(2000, params.length());
    }

    @Test
    @DisplayName("ip comes from the first X-Forwarded-For segment")
    void ipComesFromForwardedFor() throws Exception {
        String username = E2E_PREFIX + "ip";
        mockMvc.perform(post("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("X-Forwarded-For", "9.9.9.9, 10.0.0.1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createUser(username, "123456"))))
                .andExpect(status().isOk());

        assertEquals("9.9.9.9", latestLog().get("ip"));
    }

    @Test
    @DisplayName("query filters by module/status/operator and the detail endpoint returns full params")
    void queryFiltersAndDetail() throws Exception {
        String username = E2E_PREFIX + "query";
        postUser(createUser(username, "123456")).andExpect(status().isOk());

        mockMvc.perform(get("/api/logs/oper")
                        .param("module", "user")
                        .param("status", "1")
                        .param("operator", "admin")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.count").value(1))
                .andExpect(jsonPath("$.data.list[0].module").value("user"))
                .andExpect(jsonPath("$.data.list[0].status").value(1))
                .andExpect(jsonPath("$.data.list[0].operator").value("admin"));

        Long id = jdbcTemplate.queryForObject("SELECT MAX(id) FROM sys_oper_log", Long.class);
        mockMvc.perform(get("/api/logs/oper/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.params").isNotEmpty())
                .andExpect(jsonPath("$.data.params").value(org.hamcrest.Matchers.containsString(username)));
    }

    @Test
    @DisplayName("query filters exclude rows outside the time range")
    void queryFiltersByTimeRange() throws Exception {
        postUser(createUser(E2E_PREFIX + "time", "123456")).andExpect(status().isOk());

        mockMvc.perform(get("/api/logs/oper")
                        .param("from", "2000-01-01T00:00:00")
                        .param("to", "2000-01-02T00:00:00")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(0));
    }

    @Test
    @DisplayName("every write endpoint on the user/role/menu/config/dict/job controllers is annotated")
    void writeEndpointsAreAnnotated() {
        List<Class<?>> controllers = List.of(
                UserController.class, RoleController.class, MenuController.class,
                ConfigController.class, DictController.class, JobController.class);
        List<String> missing = new ArrayList<>();
        for (Class<?> controller : controllers) {
            boolean anyAnnotated = false;
            for (Method method : controller.getDeclaredMethods()) {
                boolean writeEndpoint = method.isAnnotationPresent(org.springframework.web.bind.annotation.PostMapping.class)
                        || method.isAnnotationPresent(org.springframework.web.bind.annotation.PutMapping.class)
                        || method.isAnnotationPresent(org.springframework.web.bind.annotation.DeleteMapping.class);
                if (method.isAnnotationPresent(OperLog.class)) {
                    anyAnnotated = true;
                }
                if (writeEndpoint && !method.isAnnotationPresent(OperLog.class)) {
                    missing.add(controller.getSimpleName() + "#" + method.getName());
                }
            }
            assertTrue(anyAnnotated, controller.getSimpleName() + " has no @OperLog method");
        }
        assertTrue(missing.isEmpty(), "write endpoints missing @OperLog: " + missing);
    }
}
