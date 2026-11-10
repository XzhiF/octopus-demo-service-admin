package com.octopus.demo.admin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Testcontainers
@DisplayName("schema.sql bootstraps RBAC tables and demo seed data")
class SchemaBootstrapTest {

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
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("all five RBAC tables exist")
    void fiveRbacTablesExist() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()",
                String.class);
        assertThat(tables).contains("sys_user", "sys_role", "sys_menu", "sys_user_role", "sys_role_menu");
    }

    @Test
    @DisplayName("BootstrapDataInitializer writes admin and guest with hashed passwords")
    void seedUsersExist() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_user", Integer.class);
        assertThat(count).isEqualTo(2);

        List<String> usernames = jdbcTemplate.queryForList(
                "SELECT username FROM sys_user ORDER BY username", String.class);
        assertThat(usernames).containsExactly("admin", "guest");

        List<String> hashes = jdbcTemplate.queryForList(
                "SELECT password_hash FROM sys_user", String.class);
        assertThat(hashes).allSatisfy(hash -> {
            assertThat(hash).startsWith("pbkdf2$");
            assertThat(hash).isNotEqualTo("123456");
        });

        Integer bindings = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_user_role", Integer.class);
        assertThat(bindings).isEqualTo(2);
    }

    @Test
    @DisplayName("roles and role-menu relations are seeded")
    void roleMenuSeedExists() {
        Integer roleCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_role", Integer.class);
        assertThat(roleCount).isEqualTo(2);

        Integer roleMenuCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_role_menu", Integer.class);
        assertThat(roleMenuCount).isGreaterThan(0);

        List<String> guestMenus = jdbcTemplate.queryForList(
                "SELECT m.name FROM sys_menu m JOIN sys_role_menu rm ON rm.menu_id = m.id "
                        + "JOIN sys_role r ON r.id = rm.role_id WHERE r.code = 'guest' ORDER BY m.sort",
                String.class);
        assertThat(guestMenus).containsExactly("系统管理", "用户管理");

        List<String> adminMenuTypes = jdbcTemplate.queryForList(
                "SELECT DISTINCT m.type FROM sys_menu m JOIN sys_role_menu rm ON rm.menu_id = m.id "
                        + "JOIN sys_role r ON r.id = rm.role_id WHERE r.code = 'admin'",
                String.class);
        assertThat(adminMenuTypes).contains("catalog", "menu", "button");
    }
}
