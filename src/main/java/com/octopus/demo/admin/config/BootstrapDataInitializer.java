package com.octopus.demo.admin.config;

import com.octopus.demo.common.util.PasswordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Seeds the two demo users on first startup when {@code sys_user} is empty.
 * Roles and menus come from {@code db/schema.sql}; existing users are never overwritten.
 */
@Component
public class BootstrapDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapDataInitializer.class);
    private static final String DEFAULT_PASSWORD = "123456";

    private final JdbcTemplate jdbcTemplate;

    public BootstrapDataInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_user", Integer.class);
        if (count != null && count > 0) {
            log.info("sys_user already populated ({} rows); skipping bootstrap seeding", count);
            return;
        }

        Long adminId = insertUser("admin", "管理员");
        Long guestId = insertUser("guest", "访客");
        bindRole(adminId, "admin");
        bindRole(guestId, "guest");
        log.info("Bootstrapped demo users admin and guest");
    }

    private Long insertUser(String username, String nickname) {
        jdbcTemplate.update(
                "INSERT INTO sys_user (username, password_hash, nickname, status) VALUES (?, ?, ?, 1)",
                username, PasswordUtils.encode(DEFAULT_PASSWORD), nickname);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM sys_user WHERE username = ?", Long.class, username);
    }

    private void bindRole(Long userId, String roleCode) {
        if (userId == null) {
            return;
        }
        List<Long> roleIds = jdbcTemplate.queryForList(
                "SELECT id FROM sys_role WHERE code = ?", Long.class, roleCode);
        if (roleIds.isEmpty()) {
            log.warn("Role '{}' not found in sys_role; user {} left unbound", roleCode, userId);
            return;
        }
        jdbcTemplate.update(
                "INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES (?, ?)",
                userId, roleIds.get(0));
    }
}
