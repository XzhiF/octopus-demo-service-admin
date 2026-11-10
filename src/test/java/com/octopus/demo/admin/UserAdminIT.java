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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
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
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("User admin API: CRUD, paging filters, status toggle, password reset, role grants")
class UserAdminIT {

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
    void cleanUpE2eUsers() {
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id IN "
                + "(SELECT id FROM sys_user WHERE username LIKE 'e2e%')");
        jdbcTemplate.update("DELETE FROM sys_user WHERE username LIKE 'e2e%'");
    }

    @Test
    @DisplayName("create, paged filter, duplicate rejection and guest menus use no password hash")
    void createListFilterAndMenus() throws Exception {
        String adminToken = login("admin", "123456");

        String createBody = objectMapper.writeValueAsString(userBody("e2e_user", 2));
        MvcResult created = mockMvc.perform(post("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("e2e_user"))
                .andExpect(jsonPath("$.data.status").value(1))
                .andExpect(jsonPath("$.data.roles[0].code").value("guest"))
                .andReturn();
        String createdJson = created.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(createdJson).doesNotContain("password").doesNotContain("passwordHash");

        Integer rows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user WHERE username = 'e2e_user'", Integer.class);
        assertThat(rows).isEqualTo(1);

        mockMvc.perform(get("/api/users")
                        .param("page", "1").param("size", "10")
                        .param("username", "e2e_user").param("status", "1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.count").value(1))
                .andExpect(jsonPath("$.data.list[0].username").value("e2e_user"))
                .andExpect(jsonPath("$.data.list[0].status").value(1));

        mockMvc.perform(get("/api/users")
                        .param("page", "1").param("size", "10")
                        .param("username", "e2e_user").param("status", "0")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(0));

        mockMvc.perform(post("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("用户名已存在"));

        String userToken = login("e2e_user", "123456");
        MvcResult menus = mockMvc.perform(get("/api/auth/menus")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        String menusJson = menus.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(menusJson).contains("/system/users").doesNotContain("/system/roles");
    }

    @Test
    @DisplayName("disabling a user rejects login with 401 and invalidates previously issued tokens")
    void disableUserBlocksLoginAndOldToken() throws Exception {
        String adminToken = login("admin", "123456");
        createUser(adminToken, "e2e_disabled", 2);
        long userId = userId("e2e_disabled");
        String userToken = login("e2e_disabled", "123456");

        mockMvc.perform(put("/api/users/" + userId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(0));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "e2e_disabled", "password", "123456"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.msg").value("账号已停用"));

        mockMvc.perform(get("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("built-in admin cannot be deleted or disabled, and users cannot delete themselves")
    void adminAndSelfProtection() throws Exception {
        String adminToken = login("admin", "123456");
        long adminId = userId("admin");

        mockMvc.perform(delete("/api/users/" + adminId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(put("/api/users/" + adminId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        createUser(adminToken, "e2e_self", 1);
        long selfId = userId("e2e_self");
        String selfToken = login("e2e_self", "123456");

        mockMvc.perform(delete("/api/users/" + selfId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + selfToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("不能删除自己"));
    }

    @Test
    @DisplayName("password reset rehashes credentials and role grants full-replace idempotently")
    void passwordResetAndRoleGrants() throws Exception {
        String adminToken = login("admin", "123456");
        createUser(adminToken, "e2e_reset", 2);
        long userId = userId("e2e_reset");

        mockMvc.perform(put("/api/users/" + userId + "/password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"newpass123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "e2e_reset", "password", "123456"))))
                .andExpect(status().isUnauthorized());
        login("e2e_reset", "newpass123");

        mockMvc.perform(get("/api/users/" + userId + "/roles")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("guest"));

        mockMvc.perform(put("/api/users/" + userId + "/roles")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles[0].code").value("admin"));
        assertThat(roleBindingCount(userId)).isEqualTo(1);

        mockMvc.perform(put("/api/users/" + userId + "/roles")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1,2]"))
                .andExpect(status().isOk());
        assertThat(roleBindingCount(userId)).isEqualTo(2);

        mockMvc.perform(put("/api/users/" + userId + "/roles")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[2]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles[0].code").value("guest"));
        assertThat(roleBindingCount(userId)).isEqualTo(1);
    }

    @Test
    @DisplayName("gender round-trips through create/update/read and a blank value clears it")
    void genderPersistsAndClears() throws Exception {
        String adminToken = login("admin", "123456");
        Map<String, Object> body = userBody("e2e_gender", 2);
        body.put("gender", "1");

        MvcResult created = mockMvc.perform(post("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gender").value("1"))
                .andReturn();
        long id = objectMapper.readTree(
                        created.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data").path("id").asLong();

        mockMvc.perform(put("/api/users/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "e2e_gender",
                                "nickname", "e2e_gender 昵称",
                                "gender", "2",
                                "status", 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gender").value("2"));

        mockMvc.perform(get("/api/users/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gender").value("2"));

        mockMvc.perform(put("/api/users/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "e2e_gender",
                                "nickname", "e2e_gender 昵称",
                                "gender", "",
                                "status", 1))))
                .andExpect(status().isOk());
        String stored = jdbcTemplate.queryForObject(
                "SELECT gender FROM sys_user WHERE id = ?", String.class, id);
        assertThat(stored).isNull();
    }

    private Map<String, Object> userBody(String username, long roleId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", username);
        body.put("nickname", username + " 昵称");
        body.put("password", "123456");
        body.put("status", 1);
        body.put("roleIds", List.of(roleId));
        return body;
    }

    private void createUser(String adminToken, String username, long roleId) throws Exception {
        mockMvc.perform(post("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userBody(username, roleId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(username));
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode root = objectMapper.readTree(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        String token = root.path("data").path("token").asText();
        assertThat(token).isNotBlank();
        return token;
    }

    private long userId(String username) {
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_user WHERE username = ?", Long.class, username);
        assertThat(id).isNotNull();
        return id;
    }

    private int roleBindingCount(long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user_role WHERE user_id = ?", Integer.class, userId);
        return count == null ? 0 : count;
    }
}
