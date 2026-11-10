package com.octopus.demo.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.octopus.demo.admin.schedule.JobSyncService;
import com.octopus.demo.admin.schedule.ScheduledTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.quartz.JobKey;
import org.quartz.Scheduler;
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
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Scheduled job CRUD, cron/bean validation and Quartz synchronization")
class JobAdminIT {

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
        ScheduledTask e2eTestTask() {
            return params -> {
                EXECUTIONS.incrementAndGet();
                return "ok:" + (params == null ? "" : params);
            };
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private JobSyncService jobSyncService;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        ScheduledTaskStubs.EXECUTIONS.set(0);
        cleanUp();
        token = login("admin");
    }

    @AfterEach
    void cleanUp() {
        ScheduledTaskStubs.EXECUTIONS.set(0);
        List<Map<String, Object>> jobs = jdbcTemplate.queryForList(
                "SELECT job_name, job_group FROM sys_job WHERE job_name LIKE 'e2e%'");
        for (Map<String, Object> job : jobs) {
            try {
                scheduler.deleteJob(new JobKey((String) job.get("job_name"), (String) job.get("job_group")));
            } catch (Exception ignored) {
                // the scheduler may already have been cleared by the restart test
            }
        }
        jdbcTemplate.update("DELETE FROM sys_job_log WHERE job_name LIKE 'e2e%'");
        jdbcTemplate.update("DELETE FROM sys_job WHERE job_name LIKE 'e2e%'");
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

    private Map<String, Object> jobPayload(String name, String beanName, String cron, int status) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("jobName", name);
        payload.put("jobGroup", "DEFAULT");
        payload.put("beanName", beanName);
        payload.put("cron", cron);
        payload.put("status", status);
        return payload;
    }

    private JsonNode postJob(Map<String, Object> payload) throws Exception {
        String response = mockMvc.perform(post("/api/jobs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(response);
    }

    private long createJob(String name, String cron, int status) throws Exception {
        JsonNode created = postJob(jobPayload(name, "e2eTestTask", cron, status));
        assertThat(created.path("code").asInt()).isEqualTo(200);
        return created.path("data").path("id").asLong();
    }

    private Integer triggerCount(String jobName) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM QRTZ_TRIGGERS WHERE JOB_NAME = ?", Integer.class, jobName);
    }

    @Test
    @DisplayName("create persists sys_job and registers a non-concurrent DoNothing trigger")
    void createPersistsAndRegistersTrigger() throws Exception {
        JsonNode created = postJob(jobPayload("e2e_job_a", "e2eTestTask", "0/5 * * * * ?", 1));

        assertThat(created.path("code").asInt()).isEqualTo(200);
        assertThat(created.path("data").path("id").asLong()).isPositive();
        assertThat(created.path("data").path("cron").asText()).isEqualTo("0/5 * * * * ?");

        Integer rows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_job WHERE job_name = 'e2e_job_a'", Integer.class);
        assertThat(rows).isEqualTo(1);
        assertThat(triggerCount("e2e_job_a")).isEqualTo(1);

        String expression = jdbcTemplate.queryForObject(
                "SELECT CRON_EXPRESSION FROM QRTZ_CRON_TRIGGERS WHERE TRIGGER_NAME = 'e2e_job_a'",
                String.class);
        assertThat(expression).isEqualTo("0/5 * * * * ?");

        Integer misfire = jdbcTemplate.queryForObject(
                "SELECT MISFIRE_INSTR FROM QRTZ_TRIGGERS WHERE JOB_NAME = 'e2e_job_a'", Integer.class);
        assertThat(misfire).isEqualTo(2);

        String nonConcurrent = jdbcTemplate.queryForObject(
                "SELECT IS_NONCONCURRENT FROM QRTZ_JOB_DETAILS WHERE JOB_NAME = 'e2e_job_a'", String.class);
        assertThat(nonConcurrent).isEqualTo("1");
    }

    @Test
    @DisplayName("an invalid cron is rejected with 400 and a reason")
    void invalidCronReturns400() throws Exception {
        mockMvc.perform(post("/api/jobs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                jobPayload("e2e_job_bad_cron", "e2eTestTask", "not-a-cron", 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("cron")));

        Integer rows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_job WHERE job_name = 'e2e_job_bad_cron'", Integer.class);
        assertThat(rows).isZero();
    }

    @Test
    @DisplayName("a bean that does not implement ScheduledTask is rejected with 400")
    void unknownBeanReturns400() throws Exception {
        mockMvc.perform(post("/api/jobs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                jobPayload("e2e_job_bad_bean", "noSuchBean", "0/5 * * * * ?", 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("noSuchBean")));

        Integer rows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_job WHERE job_name = 'e2e_job_bad_bean'", Integer.class);
        assertThat(rows).isZero();
    }

    @Test
    @DisplayName("updating cron reschedules the trigger in Quartz")
    void updateCronReschedules() throws Exception {
        long id = createJob("e2e_job_update", "0/10 * * * * ?", 1);

        mockMvc.perform(put("/api/jobs/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                jobPayload("e2e_job_update", "e2eTestTask", "0/15 * * * * ?", 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        String stored = jdbcTemplate.queryForObject(
                "SELECT cron FROM sys_job WHERE id = ?", String.class, id);
        assertThat(stored).isEqualTo("0/15 * * * * ?");

        String expression = jdbcTemplate.queryForObject(
                "SELECT CRON_EXPRESSION FROM QRTZ_CRON_TRIGGERS WHERE TRIGGER_NAME = 'e2e_job_update'",
                String.class);
        assertThat(expression).isEqualTo("0/15 * * * * ?");
        assertThat(triggerCount("e2e_job_update")).isEqualTo(1);
    }

    @Test
    @DisplayName("delete removes the row and its Quartz trigger")
    void deleteRemovesRowAndTrigger() throws Exception {
        long id = createJob("e2e_job_delete", "0/5 * * * * ?", 1);
        assertThat(triggerCount("e2e_job_delete")).isEqualTo(1);

        mockMvc.perform(delete("/api/jobs/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer rows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_job WHERE id = ?", Integer.class, id);
        assertThat(rows).isZero();
        assertThat(triggerCount("e2e_job_delete")).isZero();
    }

    @Test
    @DisplayName("an enabled job actually fires and resolves its ScheduledTask bean")
    void enabledJobFires() throws Exception {
        createJob("e2e_job_fire", "0/1 * * * * ?", 1);

        long deadline = System.currentTimeMillis() + 15_000;
        while (ScheduledTaskStubs.EXECUTIONS.get() == 0 && System.currentTimeMillis() < deadline) {
            Thread.sleep(200);
        }
        assertThat(ScheduledTaskStubs.EXECUTIONS.get()).isPositive();
    }

    @Test
    @DisplayName("startup sync registers enabled rows only")
    void startupSyncRegistersEnabledRowsOnly() throws Exception {
        createJob("e2e_job_on", "0/30 * * * * ?", 1);
        createJob("e2e_job_off", "0/30 * * * * ?", 0);
        assertThat(triggerCount("e2e_job_on")).isEqualTo(1);
        assertThat(triggerCount("e2e_job_off")).isZero();

        scheduler.clear();
        assertThat(triggerCount("e2e_job_on")).isZero();

        jobSyncService.syncAll();

        assertThat(triggerCount("e2e_job_on")).isEqualTo(1);
        assertThat(triggerCount("e2e_job_off")).isZero();
    }

    @Test
    @DisplayName("renaming a job removes the old trigger and registers the new key")
    void renameMovesTriggerToNewKey() throws Exception {
        long id = createJob("e2e_job_old", "0/30 * * * * ?", 1);
        assertThat(triggerCount("e2e_job_old")).isEqualTo(1);

        mockMvc.perform(put("/api/jobs/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                jobPayload("e2e_job_new", "e2eTestTask", "0/30 * * * * ?", 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        assertThat(triggerCount("e2e_job_old")).isZero();
        assertThat(triggerCount("e2e_job_new")).isEqualTo(1);
    }

    @Test
    @DisplayName("list exposes the most recent execution time")
    void listExposesLastRunAt() throws Exception {
        long id = createJob("e2e_job_lastrun", "0 0/30 * * * ?", 0);
        jdbcTemplate.update(
                "INSERT INTO sys_job_log (job_id, job_name, status, message, started_at, finished_at, duration_ms, trigger_type)"
                        + " VALUES (?, 'e2e_job_lastrun', 1, 'ok', ?, ?, 1, 'manual')",
                id, Timestamp.valueOf(LocalDateTime.of(2026, 2, 3, 4, 5, 6)),
                Timestamp.valueOf(LocalDateTime.of(2026, 2, 3, 4, 5, 7)));

        mockMvc.perform(get("/api/jobs")
                        .param("jobName", "e2e_job_lastrun")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list[0].lastRunAt")
                        .value(org.hamcrest.Matchers.startsWith("2026-02-03T04:05:06")));
    }

    @Test
    @DisplayName("list paginates and filters by name and status")
    void listPaginatesAndFilters() throws Exception {
        createJob("e2e_job_alpha", "0/30 * * * * ?", 1);
        createJob("e2e_job_beta", "0/30 * * * * ?", 0);

        mockMvc.perform(get("/api/jobs")
                        .param("page", "1")
                        .param("size", "10")
                        .param("jobName", "e2e_")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.count").value(2));

        mockMvc.perform(get("/api/jobs")
                        .param("page", "1")
                        .param("size", "10")
                        .param("jobName", "e2e_")
                        .param("status", "0")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(1))
                .andExpect(jsonPath("$.data.list[0].jobName").value("e2e_job_beta"))
                .andExpect(jsonPath("$.data.list[0].status").value(0));

        mockMvc.perform(get("/api/jobs")
                        .param("page", "1")
                        .param("size", "1")
                        .param("jobName", "e2e_")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(2))
                .andExpect(jsonPath("$.data.list.length()").value(1));
    }
}
