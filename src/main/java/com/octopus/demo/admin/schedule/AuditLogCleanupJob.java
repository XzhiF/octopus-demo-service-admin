package com.octopus.demo.admin.schedule;

import com.octopus.demo.admin.exception.NotFoundException;
import com.octopus.demo.admin.service.ConfigService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Deletes login and operation audit logs older than the configured retention window. */
@Component("auditLogCleanupJob")
public class AuditLogCleanupJob implements ScheduledTask {

    static final String RETENTION_KEY = "sys.log.retention-days";
    static final int DEFAULT_RETENTION_DAYS = 30;

    private final ConfigService configService;
    private final JdbcTemplate jdbcTemplate;

    public AuditLogCleanupJob(ConfigService configService, JdbcTemplate jdbcTemplate) {
        this.configService = configService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String execute(String params) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays());
        int loginDeleted = jdbcTemplate.update("DELETE FROM sys_login_log WHERE login_at < ?", cutoff);
        int operDeleted = jdbcTemplate.update("DELETE FROM sys_oper_log WHERE created_at < ?", cutoff);
        return "loginDeleted=" + loginDeleted + ", operDeleted=" + operDeleted;
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
