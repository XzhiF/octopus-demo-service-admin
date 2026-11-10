package com.octopus.demo.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Role admin API: CRUD, built-in guards, occupancy guard and menu grants")
class RoleAdminIT {

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

    @BeforeEach
    @AfterEach
    void cleanupE2eData() {
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id IN "
                + "(SELECT id FROM sys_user WHERE username LIKE 'e2e_%')");
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE role_id IN "
                + "(SELECT id FROM sys_role WHERE code LIKE 'e2e_%')");
        jdbcTemplate.update("DELETE FROM sys_role_menu WHERE role_id IN "
                + "(SELECT id FROM sys_role WHERE code LIKE 'e2e_%')");
        jdbcTemplate.update("DELETE FROM sys_user WHERE username LIKE 'e2e_%'");
        jdbcTemplate.update("DELETE FROM sys_role WHERE code LIKE 'e2e_%'");
    }

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

    private String authorized(String token) {
        return "Bearer " + token;
    }

    private long roleId(String code) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM sys_role WHERE code = ?", Long.class, code);
    }

    private List<Long> menuIds(long roleId) {
        return jdbcTemplate.queryForList(
                "SELECT menu_id FROM sys_role_menu WHERE role_id = ? ORDER BY menu_id",
                Long.class, roleId);
    }

    private void createE2eRole(String token) throws Exception {
        mockMvc.perform(post("/api/roles")
                        .header(HttpHeaders.AUTHORIZATION, authorized(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"e2e角色","code":"e2e_role"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("create persists, rejects duplicate/invalid code, filters the page and updates name")
    void createFilterAndUpdate() throws Exception {
        String token = login("admin");

        createE2eRole(token);
        Integer rows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_role WHERE code = 'e2e_role'", Integer.class);
        assertThat(rows).isEqualTo(1);

        mockMvc.perform(get("/api/roles")
                        .param("page", "1")
                        .param("size", "10")
                        .param("name", "e2e")
                        .param("status", "1")
                        .header(HttpHeaders.AUTHORIZATION, authorized(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.count").value(1))
                .andExpect(jsonPath("$.data.list[0].code").value("e2e_role"))
                .andExpect(jsonPath("$.data.list[0].status").value(1))
                .andExpect(jsonPath("$.data.list[0].builtIn").value(false));

        mockMvc.perform(post("/api/roles")
                        .header(HttpHeaders.AUTHORIZATION, authorized(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"重复角色","code":"e2e_role"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("角色编码已存在"));

        mockMvc.perform(post("/api/roles")
                        .header(HttpHeaders.AUTHORIZATION, authorized(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"非法编码","code":"E2E-Role"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        long id = roleId("e2e_role");
        mockMvc.perform(put("/api/roles/" + id)
                        .header(HttpHeaders.AUTHORIZATION, authorized(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"e2e角色改","code":"e2e_role","status":1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("e2e角色改"));

        String name = jdbcTemplate.queryForObject(
                "SELECT name FROM sys_role WHERE id = ?", String.class, id);
        assertThat(name).isEqualTo("e2e角色改");
    }

    @Test
    @DisplayName("built-in admin role cannot be deleted or disabled")
    void builtInAdminIsProtected() throws Exception {
        String token = login("admin");
        long adminRoleId = roleId("admin");

        mockMvc.perform(put("/api/roles/" + adminRoleId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, authorized(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("内置角色不可停用"));

        mockMvc.perform(delete("/api/roles/" + adminRoleId)
                        .header(HttpHeaders.AUTHORIZATION, authorized(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("内置角色不可删除"));

        Integer status = jdbcTemplate.queryForObject(
                "SELECT status FROM sys_role WHERE id = ?", Integer.class, adminRoleId);
        assertThat(status).isEqualTo(1);
    }

    @Test
    @DisplayName("deleting a role occupied by a user returns 400 with the occupant count")
    void deleteOccupiedRoleIsRejected() throws Exception {
        String token = login("admin");
        createE2eRole(token);
        long roleId = roleId("e2e_role");

        String response = mockMvc.perform(post("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, authorized(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"e2e_user","password":"123456","nickname":"端到端用户"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        long userId = objectMapper.readTree(response).path("data").path("id").asLong();
        jdbcTemplate.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)", userId, roleId);

        String deleteResponse = mockMvc.perform(delete("/api/roles/" + roleId)
                        .header(HttpHeaders.AUTHORIZATION, authorized(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        String msg = objectMapper.readTree(deleteResponse).path("msg").asText();
        assertThat(msg).contains("1").contains("占用");

        Integer remains = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_role WHERE id = ?", Integer.class, roleId);
        assertThat(remains).isEqualTo(1);
    }

    @Test
    @DisplayName("granting only 用户管理 to guest changes the guest menu tree, then restores it")
    void menuGrantReplacesGuestTree() throws Exception {
        String adminToken = login("admin");
        long guestRoleId = roleId("guest");
        List<Long> original = menuIds(guestRoleId);
        long userMenuId = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_menu WHERE name = '用户管理'", Long.class);

        try {
            mockMvc.perform(put("/api/roles/" + guestRoleId + "/menus")
                            .header(HttpHeaders.AUTHORIZATION, authorized(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(List.of(userMenuId))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            assertThat(menuIds(guestRoleId)).containsExactly(userMenuId);

            mockMvc.perform(get("/api/roles/" + guestRoleId + "/menus")
                            .header(HttpHeaders.AUTHORIZATION, authorized(adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0]").value((int) userMenuId));

            String guestToken = login("guest");
            Set<String> names = new LinkedHashSet<>();
            collectNames(menus(guestToken), names);
            assertThat(names).containsExactly("用户管理");
        } finally {
            mockMvc.perform(put("/api/roles/" + guestRoleId + "/menus")
                            .header(HttpHeaders.AUTHORIZATION, authorized(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(original)))
                    .andExpect(status().isOk());
        }

        assertThat(menuIds(guestRoleId)).containsExactlyElementsOf(original);

        Set<String> restored = new LinkedHashSet<>();
        collectNames(menus(login("guest")), restored);
        assertThat(restored).containsExactlyInAnyOrder("系统管理", "用户管理");
    }

    private JsonNode menus(String token) throws Exception {
        String response = mockMvc.perform(get("/api/auth/menus")
                        .header(HttpHeaders.AUTHORIZATION, authorized(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(response).path("data");
    }

    private void collectNames(JsonNode nodes, Set<String> names) {
        for (JsonNode node : nodes) {
            names.add(node.path("name").asText());
            collectNames(node.path("children"), names);
        }
    }
}
