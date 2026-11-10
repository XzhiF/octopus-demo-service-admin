package com.octopus.demo.admin.schedule;

import com.octopus.demo.admin.exception.NotFoundException;
import com.octopus.demo.admin.service.ConfigService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Deletes {@code sys_job_log} rows older than the configured retention window. */
@Component("jobLogCleanupJob")
public class JobLogCleanupJob implements ScheduledTask {

    static final String RETENTION_KEY = "sys.job.log.retention-days";
    static final int DEFAULT_RETENTION_DAYS = 7;

    private final ConfigService configService;
    private final JdbcTemplate jdbcTemplate;

    public JobLogCleanupJob(ConfigService configService, JdbcTemplate jdbcTemplate) {
        this.configService = configService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String execute(String params) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays());
        int deleted = jdbcTemplate.update("DELETE FROM sys_job_log WHERE started_at < ?", cutoff);
        return "deleted=" + deleted;
    }

    int retentionDays() {
        String raw;
        try {
            raw = configService.getValue(RETENTION_KEY);
        } catch (NotFoundException ex) {
            return DEFAULT_RETENTION_DAYS;
        }
        if (raw == null) {
            return DEFAULT_RETENTION_DAYS;
        }
        try {
            int days = Integer.parseInt(raw.trim());
            return days > 0 ? days : DEFAULT_RETENTION_DAYS;
        } catch (NumberFormatException ex) {
            return DEFAULT_RETENTION_DAYS;
        }
    }
}
