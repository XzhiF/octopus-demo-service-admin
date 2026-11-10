package com.octopus.demo.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.octopus.demo.admin.schedule.ScheduledTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Job pause / resume / run-now actions and their execution logs")
class JobActionIT {

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

    @TestConfiguration
    static class ScheduledTaskStubs {
        static final AtomicInteger EXECUTIONS = new AtomicInteger();

        @Bean
        ScheduledTask actionTestTask() {
            return params -> {
                EXECUTIONS.incrementAndGet();
                return "ok:" + (params == null ? "" : params);
            };
        }

        @Bean
        ScheduledTask failingTask() {
            return params -> {
                throw new IllegalStateException("boom: " + "x".repeat(600));
            };
        }
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
        ScheduledTaskStubs.EXECUTIONS.set(0);
        cleanUp();
        token = login();
    }

    @AfterEach
    void cleanUp() {
        ScheduledTaskStubs.EXECUTIONS.set(0);
        jdbcTemplate.update("DELETE FROM sys_job_log WHERE job_name LIKE 'e2e%'");
        jdbcTemplate.update("DELETE FROM sys_job WHERE job_name LIKE 'e2e%'");
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

    private long createJob(String name, String beanName, String cron, int status) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("jobName", name);
        payload.put("jobGroup", "DEFAULT");
        payload.put("beanName", beanName);
        payload.put("cron", cron);
        payload.put("status", status);
        String response = mockMvc.perform(post("/api/jobs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode created = objectMapper.readTree(response);
        assertThat(created.path("code").asInt()).isEqualTo(200);
        return created.path("data").path("id").asLong();
    }

    private void action(long id, String action) throws Exception {
        mockMvc.perform(post("/api/jobs/" + id + "/" + action)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    private List<Map<String, Object>> logsFor(long jobId) {
        return jdbcTemplate.queryForList(
                "SELECT * FROM sys_job_log WHERE job_id = ? ORDER BY id", jobId);
    }

    private int cronLogCount(long jobId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_job_log WHERE job_id = ? AND trigger_type = 'cron'",
                Integer.class, jobId);
        return count == null ? 0 : count;
    }

    private void awaitLogCount(long jobId, int expected, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (cronLogCount(jobId) < expected && System.currentTimeMillis() < deadline) {
            Thread.sleep(200);
        }
        assertThat(cronLogCount(jobId)).isGreaterThanOrEqualTo(expected);
    }

    @Test
    @DisplayName("an enabled job produces cron execution logs")
    void enabledJobProducesCronLogs() throws Exception {
        long id = createJob("e2e_job_b", "actionTestTask", "0/5 * * * * ?", 1);

        awaitLogCount(id, 2, 20_000);

        List<Map<String, Object>> logs = logsFor(id);
        assertThat(logs.get(0).get("trigger_type")).isEqualTo("cron");
        assertThat(((Number) logs.get(0).get("status")).intValue()).isEqualTo(1);
        assertThat(logs.get(0).get("job_name")).isEqualTo("e2e_job_b");
        assertThat(logs.get(0).get("started_at")).isNotNull();
        assertThat(logs.get(0).get("finished_at")).isNotNull();
    }

    @Test
    @DisplayName("pause parks the job with no new logs, resume continues on the next cron cycle")
    void pauseStopsLogsAndResumeRestarts() throws Exception {
        long id = createJob("e2e_job_pause", "actionTestTask", "0/5 * * * * ?", 1);
        awaitLogCount(id, 1, 20_000);

        mockMvc.perform(post("/api/jobs/" + id + "/pause")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(0));
        Thread.sleep(1_000);
        int afterPause = cronLogCount(id);

        Thread.sleep(11_000);
        assertThat(cronLogCount(id)).isEqualTo(afterPause);

        mockMvc.perform(post("/api/jobs/" + id + "/resume")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(1));

        long deadline = System.currentTimeMillis() + 20_000;
        while (cronLogCount(id) <= afterPause && System.currentTimeMillis() < deadline) {
            Thread.sleep(200);
        }
        assertThat(cronLogCount(id)).isGreaterThan(afterPause);
    }

    @Test
    @DisplayName("run-now adds exactly one manual log without changing cron or status")
    void runNowAddsManualLogWithoutChangingSchedule() throws Exception {
        long id = createJob("e2e_job_run", "actionTestTask", "0 0/30 * * * ?", 1);

        action(id, "run");

        long deadline = System.currentTimeMillis() + 10_000;
        while (logsFor(id).isEmpty() && System.currentTimeMillis() < deadline) {
            Thread.sleep(200);
        }
        List<Map<String, Object>> logs = logsFor(id);
        assertThat(logs).isNotEmpty();
        assertThat(logs.get(logs.size() - 1).get("trigger_type")).isEqualTo("manual");

        String cron = jdbcTemplate.queryForObject("SELECT cron FROM sys_job WHERE id = ?", String.class, id);
        Integer status = jdbcTemplate.queryForObject("SELECT status FROM sys_job WHERE id = ?", Integer.class, id);
        assertThat(cron).isEqualTo("0 0/30 * * * ?");
        assertThat(status).isEqualTo(1);
    }

    @Test
    @DisplayName("run-now on a paused job still records a manual log and leaves it paused")
    void runNowWorksWhilePaused() throws Exception {
        long id = createJob("e2e_job_run_paused", "actionTestTask", "0/5 * * * * ?", 0);

        action(id, "run");

        long deadline = System.currentTimeMillis() + 10_000;
        while (logsFor(id).isEmpty() && System.currentTimeMillis() < deadline) {
            Thread.sleep(200);
        }
        assertThat(logsFor(id)).hasSize(1);
        assertThat(logsFor(id).get(0).get("trigger_type")).isEqualTo("manual");
        Integer status = jdbcTemplate.queryForObject("SELECT status FROM sys_job WHERE id = ?", Integer.class, id);
        assertThat(status).isZero();
    }

    @Test
    @DisplayName("pause, resume and run return 404 for a missing job")
    void actionsReturn404ForMissingJob() throws Exception {
        for (String action : List.of("pause", "resume", "run")) {
            mockMvc.perform(post("/api/jobs/999999/" + action)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(404));
        }
    }

    @Test
    @DisplayName("a failing task writes a failure log with the exception message truncated to 500")
    void failingTaskWritesTruncatedFailureLog() throws Exception {
        long id = createJob("e2e_job_fail", "failingTask", "0 0/30 * * * ?", 1);

        action(id, "run");

        long deadline = System.currentTimeMillis() + 10_000;
        while (logsFor(id).isEmpty() && System.currentTimeMillis() < deadline) {
            Thread.sleep(200);
        }
        Map<String, Object> log = logsFor(id).get(0);
        assertThat(((Number) log.get("status")).intValue()).isZero();
        String message = (String) log.get("message");
        assertThat(message).contains("boom");
        assertThat(message.length()).isLessThanOrEqualTo(500);
        assertThat(log.get("trigger_type")).isEqualTo("manual");
    }

    @Test
    @DisplayName("log query page lists the latest execution for the job")
    void logQueryReturnsJobLogs() throws Exception {
        long id = createJob("e2e_job_query", "actionTestTask", "0 0/30 * * * ?", 1);
        action(id, "run");

        long deadline = System.currentTimeMillis() + 10_000;
        while (logsFor(id).isEmpty() && System.currentTimeMillis() < deadline) {
            Thread.sleep(200);
        }

        mockMvc.perform(get("/api/jobs/logs")
                        .param("jobId", String.valueOf(id))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.count").value(1))
                .andExpect(jsonPath("$.data.list[0].jobId").value(id))
                .andExpect(jsonPath("$.data.list[0].triggerType").value("manual"));
    }
}
