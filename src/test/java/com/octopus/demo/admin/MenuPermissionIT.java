package com.octopus.demo.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Menu tree and method-level permissions match the database")
class MenuPermissionIT {

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
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String login(String username) throws Exception {
        String body = objectMapper.writeValueAsString(
                java.util.Map.of("username", username, "password", "123456"));
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(response).path("data").path("token").asText();
    }

    private JsonNode menus(String token) throws Exception {
        String response = mockMvc.perform(get("/api/auth/menus")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(response).path("data");
    }

    @Test
    @DisplayName("admin sees 系统管理 with 用户管理/角色管理/菜单管理 children")
    void adminSeesAllMenus() throws Exception {
        JsonNode data = menus(login("admin"));

        assertThat(data.size()).isEqualTo(1);
        JsonNode system = data.get(0);
        assertThat(system.path("name").asText()).isEqualTo("系统管理");
        assertThat(system.path("path").asText()).isEqualTo("/system");
        assertThat(system.path("children").size()).isEqualTo(3);
        List<String> childNames = new ArrayList<>();
        system.path("children").forEach(child -> childNames.add(child.path("name").asText()));
        assertThat(childNames).containsExactly("用户管理", "角色管理", "菜单管理");
    }

    @Test
    @DisplayName("guest sees only 用户管理 and never any button node")
    void guestSeesGrantedMenus() throws Exception {
        JsonNode data = menus(login("guest"));

        assertThat(data.size()).isEqualTo(1);
        JsonNode system = data.get(0);
        assertThat(system.path("name").asText()).isEqualTo("系统管理");
        assertThat(system.path("children").size()).isEqualTo(1);
        assertThat(system.path("children").get(0).path("name").asText()).isEqualTo("用户管理");

        Set<String> names = new LinkedHashSet<>();
        collectNames(data, names);
        assertThat(names).containsExactlyInAnyOrder("系统管理", "用户管理");
    }

    @Test
    @DisplayName("API menu names equal the sys_role_menu join result for guest")
    void guestMenusMatchDatabase() throws Exception {
        Set<String> apiNames = new LinkedHashSet<>();
        collectNames(menus(login("guest")), apiNames);

        List<String> dbNames = jdbcTemplate.queryForList(
                "SELECT m.name FROM sys_menu m "
                        + "JOIN sys_role_menu rm ON rm.menu_id = m.id "
                        + "JOIN sys_role r ON r.id = rm.role_id "
                        + "JOIN sys_user_role ur ON ur.role_id = r.id "
                        + "JOIN sys_user u ON u.id = ur.user_id "
                        + "WHERE u.username = ? AND m.type IN ('catalog', 'menu') "
                        + "AND m.visible = 1 AND m.status = 1",
                String.class, "guest");

        assertThat(apiNames).containsExactlyInAnyOrderElementsOf(dbNames);
    }

    @Test
    @DisplayName("guest cannot write users/roles/resources; anonymous GET hits 401; admin GET succeeds")
    void userEndpointsEnforcePermissions() throws Exception {
        String guestToken = login("guest");
        mockMvc.perform(post("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + guestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"e2e_probe","password":"123456"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.msg").isNotEmpty());

        mockMvc.perform(put("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + guestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":2,"username":"guest"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        mockMvc.perform(post("/api/users/2/roles")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + guestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1]"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        mockMvc.perform(post("/api/roles")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + guestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"e2e_role","code":"e2e_role"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        mockMvc.perform(post("/api/resources")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + guestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"e2e_menu","type":"button","code":"e2e:probe"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        Integer roleProbe = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_role WHERE code = 'e2e_role'", Integer.class);
        assertThat(roleProbe).isZero();
        Integer guestRoleCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user_role WHERE user_id = 2", Integer.class);
        assertThat(guestRoleCount).isEqualTo(1);

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        String adminToken = login("admin");
        mockMvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer probeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user WHERE username = 'e2e_probe'", Integer.class);
        assertThat(probeCount).isZero();
    }

    private void collectNames(JsonNode nodes, Set<String> names) {
        for (JsonNode node : nodes) {
            names.add(node.path("name").asText());
            collectNames(node.path("children"), names);
        }
    }
}
