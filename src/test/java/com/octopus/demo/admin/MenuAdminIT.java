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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
@DisplayName("Menu admin tree, validation and delete constraints")
class MenuAdminIT {

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

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        cleanUp();
        token = login("admin");
    }

    @AfterEach
    void cleanUp() {
        List<Long> ids = jdbcTemplate.queryForList(
                "SELECT id FROM sys_menu WHERE name LIKE 'e2e[_]%'", Long.class);
        for (Long id : ids) {
            jdbcTemplate.update("DELETE FROM sys_role_menu WHERE menu_id = ?", id);
            jdbcTemplate.update("DELETE FROM sys_menu WHERE id = ?", id);
        }
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

    private JsonNode performPost(Map<String, Object> payload) throws Exception {
        String response = mockMvc.perform(post("/api/menus")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(response);
    }

    private JsonNode performPut(long id, Map<String, Object> payload) throws Exception {
        String response = mockMvc.perform(put("/api/menus/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(response);
    }

    @Test
    @DisplayName("tree returns 系统管理 -> 用户管理/角色管理/菜单管理 with button children")
    void treeHasThreeLevels() throws Exception {
        String response = mockMvc.perform(get("/api/menus/tree")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode data = objectMapper.readTree(response).path("data");

        JsonNode system = null;
        for (JsonNode root : data) {
            if ("系统管理".equals(root.path("name").asText())) {
                system = root;
            }
        }
        assertThat(system).isNotNull();
        assertThat(system.path("type").asText()).isEqualTo("catalog");
        assertThat(system.path("children").size()).isEqualTo(3);

        List<String> menuNames = new ArrayList<>();
        system.path("children").forEach(child -> {
            menuNames.add(child.path("name").asText());
            assertThat(child.path("type").asText()).isEqualTo("menu");
            assertThat(child.path("children").size()).isGreaterThan(0);
            child.path("children").forEach(button ->
                    assertThat(button.path("type").asText()).isEqualTo("button"));
        });
        assertThat(menuNames).containsExactly("用户管理", "角色管理", "菜单管理");
    }

    @Test
    @DisplayName("menu without path and button without perms are rejected with 400")
    void requiredFieldsPerType() throws Exception {
        JsonNode missingPath = performPost(Map.of(
                "parentId", 1, "name", "e2e_menu_no_path", "type", "menu"));
        assertThat(missingPath.path("code").asInt()).isEqualTo(400);

        JsonNode missingPerms = performPost(Map.of(
                "parentId", 4, "name", "e2e_button_no_perms", "type", "button"));
        assertThat(missingPerms.path("code").asInt()).isEqualTo(400);

        JsonNode badType = performPost(Map.of(
                "name", "e2e_bad_type", "type", "page"));
        assertThat(badType.path("code").asInt()).isEqualTo(400);

        Integer created = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_menu WHERE name LIKE 'e2e[_]%'", Integer.class);
        assertThat(created).isZero();
    }

    @Test
    @DisplayName("create e2e_menu persists and duplicate perms returns 400")
    void createMenuAndPermsUnique() throws Exception {
        JsonNode created = performPost(Map.of(
                "parentId", 1, "name", "e2e_menu", "type", "menu",
                "path", "/e2e/menu", "perms", "e2e:view", "sort", 9));
        assertThat(created.path("code").asInt()).isEqualTo(200);
        long id = created.path("data").path("id").asLong();
        assertThat(id).isPositive();

        Integer rows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_menu WHERE name = 'e2e_menu'", Integer.class);
        assertThat(rows).isEqualTo(1);

        JsonNode duplicate = performPost(Map.of(
                "parentId", 1, "name", "e2e_menu_dup", "type", "menu",
                "path", "/e2e/dup", "perms", "e2e:view"));
        assertThat(duplicate.path("code").asInt()).isEqualTo(400);

        mockMvc.perform(delete("/api/menus/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
        Integer remaining = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_menu WHERE id = ?", Integer.class, id);
        assertThat(remaining).isZero();
    }

    @Test
    @DisplayName("parent must exist and updates cannot form a cycle")
    void parentMustExistAndRemainAcyclic() throws Exception {
        JsonNode orphan = performPost(Map.of(
                "parentId", 999_999, "name", "e2e_orphan", "type", "menu",
                "path", "/e2e/orphan"));
        assertThat(orphan.path("code").asInt()).isEqualTo(400);

        JsonNode cycle = performPut(2, Map.of(
                "parentId", 11, "name", "用户管理", "type", "menu",
                "path", "/system/users", "perms", "sys:user:list"));
        assertThat(cycle.path("code").asInt()).isEqualTo(400);

        Integer parentOfUserMenu = jdbcTemplate.queryForObject(
                "SELECT parent_id FROM sys_menu WHERE id = 2", Integer.class);
        assertThat(parentOfUserMenu).isEqualTo(1);
    }

    @Test
    @DisplayName("deleting a parent or a role-referenced menu returns 400")
    void deleteConstraints() throws Exception {
        mockMvc.perform(delete("/api/menus/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(delete("/api/menus/2")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        Integer catalogStillThere = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_menu WHERE id IN (1, 2)", Integer.class);
        assertThat(catalogStillThere).isEqualTo(2);
    }
}
