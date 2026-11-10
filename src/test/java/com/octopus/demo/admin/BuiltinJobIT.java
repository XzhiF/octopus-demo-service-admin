package com.octopus.demo.admin;

import com.octopus.demo.admin.schedule.ScheduledTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@DisplayName("Built-in demo heartbeat and job log cleanup tasks")
class BuiltinJobIT {

    private static final String RETENTION_KEY = "sys.job.log.retention-days";

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
    private ApplicationContext applicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        cleanUp();
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM sys_job_log WHERE job_name LIKE 'e2e_job_%'");
        jdbcTemplate.update("DELETE FROM sys_config WHERE config_key = ?", RETENTION_KEY);
    }

    private ScheduledTask task(String beanName) {
        return applicationContext.getBean(beanName, ScheduledTask.class);
    }

    private long seedJobId(String jobName) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM sys_job WHERE job_name = ?", Long.class, jobName);
    }

    private void insertLog(long jobId, LocalDateTime startedAt) {
        jdbcTemplate.update(
                "INSERT INTO sys_job_log (job_id, job_name, status, message, started_at, finished_at, duration_ms, trigger_type)"
                        + " VALUES (?, 'e2e_job_cleanup', 1, 'ok', ?, ?, 1, 'cron')",
                jobId, startedAt, startedAt);
    }

    private void configureRetentionDays(String value) {
        jdbcTemplate.update(
                "INSERT INTO sys_config (config_key, config_value, remark) VALUES (?, ?, 'test')"
                        + " ON DUPLICATE KEY UPDATE config_value = VALUES(config_value)",
                RETENTION_KEY, value);
    }

    private int countOlderThanDays(int days) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_job_log WHERE started_at < ?",
                Integer.class, LocalDateTime.now().minusDays(days));
    }

    private int countBetweenDaysAgo(int fromDaysAgo, int toDaysAgo) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_job_log WHERE started_at >= ? AND started_at < ?",
                Integer.class,
                LocalDateTime.now().minusDays(fromDaysAgo),
                LocalDateTime.now().minusDays(toDaysAgo));
    }

    @Test
    @DisplayName("both sample jobs are seeded, paused and resolvable as ScheduledTask beans")
    void seedJobsExistPausedAndResolvable() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT job_name, bean_name, cron, status FROM sys_job"
                        + " WHERE job_name IN ('demoHeartbeatJob', 'jobLogCleanupJob')");

        assertThat(rows).hasSize(2);
        for (Map<String, Object> row : rows) {
            assertThat(((Number) row.get("status")).intValue()).isZero();
            ScheduledTask resolved = task((String) row.get("bean_name"));
            assertThat(resolved).isNotNull();
        }

        assertThat(jdbcTemplate.queryForObject(
                "SELECT cron FROM sys_job WHERE job_name = 'demoHeartbeatJob'", String.class))
                .isEqualTo("0/30 * * * * ?");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT cron FROM sys_job WHERE job_name = 'jobLogCleanupJob'", String.class))
                .isEqualTo("0 0 3 * * ?");
    }

    @Test
    @DisplayName("demoHeartbeatJob returns the heartbeat message recorded on success")
    void heartbeatReturnsHeartbeatMessage() throws Exception {
        assertThat(task("demoHeartbeatJob").execute(null)).isEqualTo("heartbeat");
    }

    @Test
    @DisplayName("cleanup removes logs older than the default 7 days and reports the count")
    void cleanupUsesDefaultSevenDayRetention() throws Exception {
        long jobId = seedJobId("jobLogCleanupJob");
        insertLog(jobId, LocalDateTime.now().minusDays(8));
        insertLog(jobId, LocalDateTime.now().minusDays(3));

        String message = task("jobLogCleanupJob").execute(null);

        assertThat(message).isEqualTo("deleted=1");
        assertThat(countOlderThanDays(7)).isZero();
        assertThat(countBetweenDaysAgo(4, 2)).isEqualTo(1);
    }

    @Test
    @DisplayName("cleanup follows sys.job.log.retention-days when it is set to 1")
    void cleanupFollowsConfiguredRetentionDays() throws Exception {
        configureRetentionDays("1");
        long jobId = seedJobId("jobLogCleanupJob");
        insertLog(jobId, LocalDateTime.now().minusDays(8));
        insertLog(jobId, LocalDateTime.now().minusDays(3));

        String message = task("jobLogCleanupJob").execute(null);

        assertThat(message).isEqualTo("deleted=2");
        assertThat(countOlderThanDays(1)).isZero();
    }

    @Test
    @DisplayName("an invalid retention value falls back to 7 days")
    void invalidRetentionFallsBackToSevenDays() throws Exception {
        configureRetentionDays("not-a-number");
        long jobId = seedJobId("jobLogCleanupJob");
        insertLog(jobId, LocalDateTime.now().minusDays(8));
        insertLog(jobId, LocalDateTime.now().minusDays(3));

        assertThat(task("jobLogCleanupJob").execute(null)).isEqualTo("deleted=1");
        assertThat(countOlderThanDays(7)).isZero();
        assertThat(countBetweenDaysAgo(4, 2)).isEqualTo(1);
    }

    @Test
    @DisplayName("a missing retention key falls back to 7 days")
    void missingRetentionFallsBackToSevenDays() throws Exception {
        jdbcTemplate.update("DELETE FROM sys_config WHERE config_key = ?", RETENTION_KEY);
        long jobId = seedJobId("jobLogCleanupJob");
        insertLog(jobId, LocalDateTime.now().minusDays(8));
        insertLog(jobId, LocalDateTime.now().minusDays(3));

        assertThat(task("jobLogCleanupJob").execute(null)).isEqualTo("deleted=1");
        assertThat(countOlderThanDays(7)).isZero();
        assertThat(countBetweenDaysAgo(4, 2)).isEqualTo(1);
    }
}
