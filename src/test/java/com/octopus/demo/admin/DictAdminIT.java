package com.octopus.demo.admin;

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
@DisplayName("Dict admin API: type CRUD, item CRUD, ordering, status filtering and delete guard")
class DictAdminIT {

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
        jdbcTemplate.update("DELETE FROM sys_dict_data WHERE dict_type LIKE 'e2e_%'");
        jdbcTemplate.update("DELETE FROM sys_dict_type WHERE dict_type LIKE 'e2e_%'");
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

    private void createType(String token, String dictType, String dictName) throws Exception {
        mockMvc.perform(post("/api/dicts/types")
                        .header(HttpHeaders.AUTHORIZATION, authorized(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dictType":"%s","dictName":"%s","status":1}
                                """.formatted(dictType, dictName)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    private long typeId(String dictType) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM sys_dict_type WHERE dict_type = ?", Long.class, dictType);
    }

    private long dataId(String dictType, String label) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM sys_dict_data WHERE dict_type = ? AND dict_label = ?",
                Long.class, dictType, label);
    }

    private long createData(String token, String dictType, String label, String value, int sort) throws Exception {
        String response = mockMvc.perform(post("/api/dicts/data")
                        .header(HttpHeaders.AUTHORIZATION, authorized(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dictType":"%s","dictLabel":"%s","dictValue":"%s","sort":%d,"status":1}
                                """.formatted(dictType, label, value, sort)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(response).path("data").path("id").asLong();
    }

    @Test
    @DisplayName("seed sys_user_sex exposes three enabled options ordered by sort")
    void seedOptionsAreSorted() throws Exception {
        String token = login("admin");

        mockMvc.perform(get("/api/dicts/data/sys_user_sex")
                        .header(HttpHeaders.AUTHORIZATION, authorized(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].label").value("男"))
                .andExpect(jsonPath("$.data[0].value").value("1"))
                .andExpect(jsonPath("$.data[1].label").value("女"))
                .andExpect(jsonPath("$.data[2].label").value("未知"));
    }

    @Test
    @DisplayName("create type persists, rejects duplicates and invalid codes, and filters the page")
    void typeCreateUniqueAndPage() throws Exception {
        String token = login("admin");

        createType(token, "e2e_dict", "端到端字典");
        Integer rows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_dict_type WHERE dict_type = 'e2e_dict'", Integer.class);
        assertThat(rows).isEqualTo(1);

        mockMvc.perform(post("/api/dicts/types")
                        .header(HttpHeaders.AUTHORIZATION, authorized(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dictType":"e2e_dict","dictName":"重复字典"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("字典类型已存在"));

        mockMvc.perform(post("/api/dicts/types")
                        .header(HttpHeaders.AUTHORIZATION, authorized(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dictType":"E2E-Dict","dictName":"非法编码"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(get("/api/dicts/types")
                        .param("page", "1")
                        .param("size", "10")
                        .param("dictType", "e2e")
                        .param("dictName", "端到端")
                        .header(HttpHeaders.AUTHORIZATION, authorized(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.count").value(1))
                .andExpect(jsonPath("$.data.list[0].dictType").value("e2e_dict"))
                .andExpect(jsonPath("$.data.list[0].dictName").value("端到端字典"));

        long id = typeId("e2e_dict");
        mockMvc.perform(put("/api/dicts/types/" + id)
                        .header(HttpHeaders.AUTHORIZATION, authorized(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dictType":"e2e_dict","dictName":"端到端字典改","status":1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dictName").value("端到端字典改"));

        String name = jdbcTemplate.queryForObject(
                "SELECT dict_name FROM sys_dict_type WHERE id = ?", String.class, id);
        assertThat(name).isEqualTo("端到端字典改");
    }

    @Test
    @DisplayName("items sort ascending, status=0 is hidden from the dropdown but kept for admin")
    void dataOrderingStatusAndDeleteGuard() throws Exception {
        String token = login("admin");
        createType(token, "e2e_dict", "端到端字典");

        createData(token, "e2e_dict", "A", "a", 3);
        createData(token, "e2e_dict", "B", "b", 1);
        createData(token, "e2e_dict", "C", "c", 2);

        mockMvc.perform(get("/api/dicts/data/e2e_dict")
                        .header(HttpHeaders.AUTHORIZATION, authorized(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].label").value("B"))
                .andExpect(jsonPath("$.data[0].value").value("b"))
                .andExpect(jsonPath("$.data[1].label").value("C"))
                .andExpect(jsonPath("$.data[2].label").value("A"));

        mockMvc.perform(get("/api/dicts/data")
                        .param("dictType", "e2e_dict")
                        .header(HttpHeaders.AUTHORIZATION, authorized(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].dictLabel").value("B"));

        long bId = dataId("e2e_dict", "B");
        mockMvc.perform(put("/api/dicts/data/" + bId)
                        .header(HttpHeaders.AUTHORIZATION, authorized(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dictType":"e2e_dict","dictLabel":"B","dictValue":"b","sort":1,"status":0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(0));

        mockMvc.perform(get("/api/dicts/data/e2e_dict")
                        .header(HttpHeaders.AUTHORIZATION, authorized(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].label").value("C"))
                .andExpect(jsonPath("$.data[1].label").value("A"));

        mockMvc.perform(get("/api/dicts/data")
                        .param("dictType", "e2e_dict")
                        .header(HttpHeaders.AUTHORIZATION, authorized(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].dictLabel").value("B"))
                .andExpect(jsonPath("$.data[0].status").value(0));

        long id = typeId("e2e_dict");
        mockMvc.perform(delete("/api/dicts/types/" + id)
                        .header(HttpHeaders.AUTHORIZATION, authorized(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("该字典类型下仍有 3 个字典项，无法删除"));

        for (String label : new String[]{"A", "B", "C"}) {
            mockMvc.perform(delete("/api/dicts/data/" + dataId("e2e_dict", label))
                            .header(HttpHeaders.AUTHORIZATION, authorized(token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        mockMvc.perform(delete("/api/dicts/types/" + id)
                        .header(HttpHeaders.AUTHORIZATION, authorized(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer remains = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_dict_type WHERE id = ?", Integer.class, id);
        assertThat(remains).isZero();
    }

    @Test
    @DisplayName("creating an item under a non-existent type returns 400")
    void createDataForMissingTypeIsRejected() throws Exception {
        String token = login("admin");

        mockMvc.perform(post("/api/dicts/data")
                        .header(HttpHeaders.AUTHORIZATION, authorized(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dictType":"e2e_missing","dictLabel":"X","dictValue":"x","sort":1,"status":1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("字典类型不存在"));
    }

    @Test
    @DisplayName("renaming a type code is rejected while the type still has items")
    void renameTypeWithItemsIsRejected() throws Exception {
        String token = login("admin");
        createType(token, "e2e_dict", "端到端字典");
        createData(token, "e2e_dict", "A", "a", 1);

        long id = typeId("e2e_dict");
        mockMvc.perform(put("/api/dicts/types/" + id)
                        .header(HttpHeaders.AUTHORIZATION, authorized(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dictType":"e2e_dict_renamed","dictName":"端到端字典","status":1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("该字典类型下仍有字典项，无法修改类型编码"));

        String stored = jdbcTemplate.queryForObject(
                "SELECT dict_type FROM sys_dict_type WHERE id = ?", String.class, id);
        assertThat(stored).isEqualTo("e2e_dict");
    }
}
