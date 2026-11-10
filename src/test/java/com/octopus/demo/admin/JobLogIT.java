package com.octopus.demo.admin;

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
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Job execution log query: filters, ordering, pagination and empty result")
class JobLogIT {

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

    private static final long JOB_A = 900001L;
    private static final long JOB_B = 900002L;

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
        insertLog(JOB_A, "e2e_log_a", 1, "a-1", LocalDateTime.of(2026, 1, 1, 10, 0), "cron");
        insertLog(JOB_B, "e2e_log_b", 1, "b-1", LocalDateTime.of(2026, 1, 1, 10, 3), "cron");
        insertLog(JOB_A, "e2e_log_a", 0, "a-fail", LocalDateTime.of(2026, 1, 1, 10, 5), "cron");
        insertLog(JOB_B, "e2e_log_b", 1, "b-2", LocalDateTime.of(2026, 1, 1, 10, 8), "manual");
        insertLog(JOB_A, "e2e_log_a", 1, "a-3", LocalDateTime.of(2026, 1, 1, 10, 10), "cron");
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM sys_job_log WHERE job_id IN (?, ?)", JOB_A, JOB_B);
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

    private void insertLog(long jobId, String jobName, int status, String message,
                           LocalDateTime startedAt, String triggerType) {
        jdbcTemplate.update(
                "INSERT INTO sys_job_log (job_id, job_name, status, message, started_at, finished_at, duration_ms, trigger_type) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                jobId, jobName, status, message,
                Timestamp.valueOf(startedAt), Timestamp.valueOf(startedAt.plusSeconds(1)), 1000L, triggerType);
    }

    @Test
    @DisplayName("filters by jobId and returns rows ordered by started_at descending")
    void filtersByJobIdNewestFirst() throws Exception {
        mockMvc.perform(get("/api/jobs/logs")
                        .param("jobId", String.valueOf(JOB_A))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.count").value(3))
                .andExpect(jsonPath("$.data.list[0].message").value("a-3"))
                .andExpect(jsonPath("$.data.list[1].message").value("a-fail"))
                .andExpect(jsonPath("$.data.list[2].message").value("a-1"));
    }

    @Test
    @DisplayName("filters by status so only failures are returned")
    void filtersByStatus() throws Exception {
        mockMvc.perform(get("/api/jobs/logs")
                        .param("status", "0")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(1))
                .andExpect(jsonPath("$.data.list[0].message").value("a-fail"))
                .andExpect(jsonPath("$.data.list[0].status").value(0))
                .andExpect(jsonPath("$.data.list[0].jobName").value("e2e_log_a"));
    }

    @Test
    @DisplayName("filters by started_at range")
    void filtersByTimeRange() throws Exception {
        mockMvc.perform(get("/api/jobs/logs")
                        .param("from", "2026-01-01T10:04:00")
                        .param("to", "2026-01-01T10:09:00")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(2))
                .andExpect(jsonPath("$.data.list[0].message").value("b-2"))
                .andExpect(jsonPath("$.data.list[1].message").value("a-fail"));
    }

    @Test
    @DisplayName("paginates the filtered result set")
    void paginates() throws Exception {
        mockMvc.perform(get("/api/jobs/logs")
                        .param("jobId", String.valueOf(JOB_A))
                        .param("page", "2")
                        .param("size", "2")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(3))
                .andExpect(jsonPath("$.data.list.length()").value(1))
                .andExpect(jsonPath("$.data.list[0].message").value("a-1"));
    }

    @Test
    @DisplayName("an unknown jobId yields an empty page")
    void unknownJobIdYieldsEmptyPage() throws Exception {
        mockMvc.perform(get("/api/jobs/logs")
                        .param("jobId", "999999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(0))
                .andExpect(jsonPath("$.data.list.length()").value(0));
    }
}
