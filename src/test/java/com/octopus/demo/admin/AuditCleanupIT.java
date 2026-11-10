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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Testcontainers
@DisplayName("Audit log retention cleanup task")
class AuditCleanupIT {

    private static final String RETENTION_KEY = "sys.log.retention-days";
    private static final String MARKER = "e2e_retention";

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
        deleteAuditLogs();
    }

    @AfterEach
    void cleanUp() {
        deleteAuditLogs();
        configureRetentionDays("30");
    }

    private void deleteAuditLogs() {
        jdbcTemplate.update("DELETE FROM sys_login_log WHERE username = ?", MARKER);
        jdbcTemplate.update("DELETE FROM sys_oper_log WHERE `operator` = ?", MARKER);
    }

    private void configureRetentionDays(String value) {
        jdbcTemplate.update(
                "INSERT INTO sys_config (config_key, config_value, remark) VALUES (?, ?, 'test')"
                        + " ON DUPLICATE KEY UPDATE config_value = VALUES(config_value)",
                RETENTION_KEY, value);
    }

    private void insertLoginLog(int daysAgo) {
        jdbcTemplate.update(
                "INSERT INTO sys_login_log (username, ip, user_agent, status, message, login_at)"
                        + " VALUES (?, '127.0.0.1', 'test-agent', 1, 'ok', ?)",
                MARKER, LocalDateTime.now().minusDays(daysAgo));
    }

    private void insertOperLog(int daysAgo) {
        jdbcTemplate.update(
                "INSERT INTO sys_oper_log (title, module, http_method, request_uri, params,"
                        + " `operator`, ip, status, error_msg, duration_ms, created_at)"
                        + " VALUES ('title', 'module', 'POST', '/api/test', '{}', ?, '127.0.0.1', 1, NULL, 1, ?)",
                MARKER, LocalDateTime.now().minusDays(daysAgo));
    }

    private int loginCountOlderThanDays(int days) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_login_log WHERE username = ? AND login_at < ?",
                Integer.class, MARKER, LocalDateTime.now().minusDays(days));
    }

    private int operCountOlderThanDays(int days) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_oper_log WHERE `operator` = ? AND created_at < ?",
                Integer.class, MARKER, LocalDateTime.now().minusDays(days));
    }

    private int loginTotalCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_login_log WHERE username = ?", Integer.class, MARKER);
    }

    private int operTotalCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_oper_log WHERE `operator` = ?", Integer.class, MARKER);
    }

    private ScheduledTask cleanupTask() {
        return applicationContext.getBean("auditLogCleanupJob", ScheduledTask.class);
    }

    private void seedMixedAges() {
        insertLoginLog(40);
        insertLoginLog(40);
        insertLoginLog(10);
        insertOperLog(40);
        insertOperLog(40);
        insertOperLog(40);
        insertOperLog(10);
    }

    @Test
    @DisplayName("retention key is seeded as 30 and the job is seeded paused with the daily 03:30 cron")
    void seedConfigAndJob() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT config_value FROM sys_config WHERE config_key = ?", String.class, RETENTION_KEY))
                .isEqualTo("30");
        // The remark is only present on the schema seed row; the test helper writes 'test'.
        assertThat(jdbcTemplate.queryForObject(
                "SELECT remark FROM sys_config WHERE config_key = ?", String.class, RETENTION_KEY))
                .contains("auditLogCleanupJob");

        Map<String, Object> job = jdbcTemplate.queryForMap(
                "SELECT bean_name, cron, status FROM sys_job WHERE job_name = 'auditLogCleanupJob'");
        assertThat(job.get("bean_name")).isEqualTo("auditLogCleanupJob");
        assertThat(job.get("cron")).isEqualTo("0 30 3 * * ?");
        assertThat(((Number) job.get("status")).intValue()).isZero();
        assertThat(cleanupTask()).isNotNull();
    }

    @Test
    @DisplayName("cleanup deletes 40-day-old rows only and reports both delete counts")
    void cleanupUsesDefaultThirtyDayRetention() throws Exception {
        seedMixedAges();

        String message = cleanupTask().execute(null);

        assertThat(message).isEqualTo("loginDeleted=2, operDeleted=3");
        assertThat(loginCountOlderThanDays(30)).isZero();
        assertThat(operCountOlderThanDays(30)).isZero();
        assertThat(loginTotalCount()).isEqualTo(1);
        assertThat(operTotalCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("cleanup follows sys.log.retention-days when it is set to 5")
    void cleanupFollowsConfiguredRetentionDays() throws Exception {
        configureRetentionDays("5");
        seedMixedAges();

        String message = cleanupTask().execute(null);

        assertThat(message).isEqualTo("loginDeleted=3, operDeleted=4");
        assertThat(loginTotalCount()).isZero();
        assertThat(operTotalCount()).isZero();
    }

    @Test
    @DisplayName("an invalid retention value falls back to 30 days without throwing")
    void invalidRetentionFallsBackToThirtyDays() throws Exception {
        configureRetentionDays("abc");
        seedMixedAges();

        String message = cleanupTask().execute(null);

        assertThat(message).isEqualTo("loginDeleted=2, operDeleted=3");
        assertThat(loginTotalCount()).isEqualTo(1);
        assertThat(operTotalCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("a non-positive retention value falls back to 30 days")
    void nonPositiveRetentionFallsBackToThirtyDays() throws Exception {
        configureRetentionDays("0");
        seedMixedAges();

        String message = cleanupTask().execute(null);

        assertThat(message).isEqualTo("loginDeleted=2, operDeleted=3");
        assertThat(loginTotalCount()).isEqualTo(1);
        assertThat(operTotalCount()).isEqualTo(1);
    }
}
