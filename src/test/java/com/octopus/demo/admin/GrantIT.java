package com.octopus.demo.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
@DisplayName("Grant service: full replace, idempotency, instant effect and cascade cleanup")
class GrantIT {

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

    @AfterEach
    void cleanupAndRestoreSeedGrants() {
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id IN "
                + "(SELECT id FROM sys_user WHERE username LIKE 'e2e_%')");
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE role_id IN "
                + "(SELECT id FROM sys_role WHERE code LIKE 'e2e_%')");
        jdbcTemplate.update("DELETE FROM sys_role_menu WHERE role_id IN "
                + "(SELECT id FROM sys_role WHERE code LIKE 'e2e_%')");
        jdbcTemplate.update("DELETE FROM sys_user WHERE username LIKE 'e2e_%'");
        jdbcTemplate.update("DELETE FROM sys_role WHERE code LIKE 'e2e_%'");

        long guestRoleId = roleId("guest");
        jdbcTemplate.update("DELETE FROM sys_role_menu WHERE role_id = ?", guestRoleId);
        jdbcTemplate.update("INSERT INTO sys_role_menu (role_id, menu_id) VALUES (?, 1), (?, 2)",
                guestRoleId, guestRoleId);
        long guestId = userId("guest");
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = ?", guestId);
        jdbcTemplate.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)",
                guestId, guestRoleId);
    }

    @Test
    @DisplayName("PUT user roles replaces fully, repeating it is idempotent and [] clears the grant")
    void userRoleGrantReplacesIdempotentlyAndClears() throws Exception {
        String adminToken = login("admin");
        long guestId = userId("guest");
        long guestRoleId = roleId("guest");
        long e2eRoleId = createE2eRole(adminToken);

        putJson(adminToken, "/api/users/" + guestId + "/roles",
                List.of(guestRoleId, e2eRoleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.roles.length()").value(2));
        assertThat(userRoleIds(guestId)).containsExactlyInAnyOrder(guestRoleId, e2eRoleId);

        putJson(adminToken, "/api/users/" + guestId + "/roles",
                List.of(guestRoleId, e2eRoleId))
                .andExpect(status().isOk());
        assertThat(userRoleIds(guestId)).hasSize(2);

        mockMvc.perform(get("/api/users/" + guestId + "/roles")
                        .header(HttpHeaders.AUTHORIZATION, authorized(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        String guestToken = login("guest");
        putJson(adminToken, "/api/users/" + guestId + "/roles", List.of())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles.length()").value(0));
        assertThat(userRoleIds(guestId)).isEmpty();

        mockMvc.perform(get("/api/auth/menus")
                        .header(HttpHeaders.AUTHORIZATION, authorized(guestToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("role menu grants drive the next request of an already logged-in guest")
    void roleMenuGrantTakesEffectOnNextRequest() throws Exception {
        String adminToken = login("admin");
        long guestRoleId = roleId("guest");
        long catalogId = menuId("系统管理");
        long userMenuId = menuId("用户管理");
        long roleMenuId = menuId("角色管理");
        String guestToken = login("guest");

        try {
            putJson(adminToken, "/api/roles/" + guestRoleId + "/menus", List.of(userMenuId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
            assertThat(roleMenuIds(guestRoleId)).containsExactly(userMenuId);

            mockMvc.perform(get("/api/auth/menus")
                            .header(HttpHeaders.AUTHORIZATION, authorized(guestToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].name").value("用户管理"));
            mockMvc.perform(get("/api/users")
                            .header(HttpHeaders.AUTHORIZATION, authorized(guestToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            putJson(adminToken, "/api/roles/" + guestRoleId + "/menus", List.of(roleMenuId))
                    .andExpect(status().isOk());
            mockMvc.perform(get("/api/users")
                            .header(HttpHeaders.AUTHORIZATION, authorized(guestToken)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(403));
            mockMvc.perform(get("/api/roles")
                            .header(HttpHeaders.AUTHORIZATION, authorized(guestToken)))
                    .andExpect(status().isOk());
            assertThat(menuNames(guestToken)).containsExactly("角色管理");

            putJson(adminToken, "/api/roles/" + guestRoleId + "/menus",
                    List.of(catalogId, userMenuId, roleMenuId))
                    .andExpect(status().isOk());
            assertThat(menuNames(guestToken))
                    .containsExactlyInAnyOrder("系统管理", "用户管理", "角色管理");
        } finally {
            putJson(adminToken, "/api/roles/" + guestRoleId + "/menus",
                    List.of(catalogId, userMenuId));
        }
    }

    @Test
    @DisplayName("missing user/role returns 404 and unknown ids inside the list return 400")
    void invalidIdsAreRejected() throws Exception {
        String adminToken = login("admin");
        long guestId = userId("guest");
        long guestRoleId = roleId("guest");
        List<Long> guestRolesBefore = userRoleIds(guestId);
        List<Long> guestMenusBefore = roleMenuIds(guestRoleId);

        putJson(adminToken, "/api/users/" + guestId + "/roles", List.of(999999L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
        assertThat(userRoleIds(guestId)).containsExactlyElementsOf(guestRolesBefore);

        putJson(adminToken, "/api/roles/" + guestRoleId + "/menus", List.of(999999L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
        assertThat(roleMenuIds(guestRoleId)).containsExactlyElementsOf(guestMenusBefore);

        putJson(adminToken, "/api/users/999999/roles", List.of())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));

        putJson(adminToken, "/api/roles/999999/menus", List.of())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("deleting a user clears sys_user_role and deleting a role clears sys_role_menu")
    void deletesCascadeAssociationRows() throws Exception {
        String adminToken = login("admin");
        long e2eRoleId = createE2eRole(adminToken);
        putJson(adminToken, "/api/roles/" + e2eRoleId + "/menus", List.of(2L, 3L))
                .andExpect(status().isOk());
        assertThat(roleMenuIds(e2eRoleId)).hasSize(2);

        mockMvc.perform(post("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, authorized(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "e2e_user",
                                "password", "123456",
                                "roleIds", List.of(e2eRoleId)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        long e2eUserId = userId("e2e_user");
        assertThat(userRoleIds(e2eUserId)).containsExactly(e2eRoleId);

        mockMvc.perform(delete("/api/users/" + e2eUserId)
                        .header(HttpHeaders.AUTHORIZATION, authorized(adminToken)))
                .andExpect(status().isOk());
        assertThat(userRoleIds(e2eUserId)).isEmpty();

        mockMvc.perform(delete("/api/roles/" + e2eRoleId)
                        .header(HttpHeaders.AUTHORIZATION, authorized(adminToken)))
                .andExpect(status().isOk());
        assertThat(roleMenuIds(e2eRoleId)).isEmpty();
        Integer roles = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_role WHERE id = ?", Integer.class, e2eRoleId);
        assertThat(roles).isZero();
    }

    private org.springframework.test.web.servlet.ResultActions putJson(
            String token, String url, List<Long> body) throws Exception {
        return mockMvc.perform(put(url)
                .header(HttpHeaders.AUTHORIZATION, authorized(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private long createE2eRole(String token) throws Exception {
        mockMvc.perform(post("/api/roles")
                        .header(HttpHeaders.AUTHORIZATION, authorized(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"e2e角色","code":"e2e_role"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        return roleId("e2e_role");
    }

    private String login(String username) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", username, "password", "123456"));
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(response).path("data").path("token").asText();
    }

    private String authorized(String token) {
        return "Bearer " + token;
    }

    private long userId(String username) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM sys_user WHERE username = ?", Long.class, username);
    }

    private long roleId(String code) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM sys_role WHERE code = ?", Long.class, code);
    }

    private long menuId(String name) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM sys_menu WHERE name = ?", Long.class, name);
    }

    private List<Long> userRoleIds(long userId) {
        return jdbcTemplate.queryForList(
                "SELECT role_id FROM sys_user_role WHERE user_id = ? ORDER BY role_id",
                Long.class, userId);
    }

    private List<Long> roleMenuIds(long roleId) {
        return jdbcTemplate.queryForList(
                "SELECT menu_id FROM sys_role_menu WHERE role_id = ? ORDER BY menu_id",
                Long.class, roleId);
    }

    private Set<String> menuNames(String token) throws Exception {
        String response = mockMvc.perform(get("/api/auth/menus")
                        .header(HttpHeaders.AUTHORIZATION, authorized(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        Set<String> names = new LinkedHashSet<>();
        collectNames(objectMapper.readTree(response).path("data"), names);
        return names;
    }

    private void collectNames(JsonNode nodes, Set<String> names) {
        for (JsonNode node : nodes) {
            names.add(node.path("name").asText());
            collectNames(node.path("children"), names);
        }
    }
}
