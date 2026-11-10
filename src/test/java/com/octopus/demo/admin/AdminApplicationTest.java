package com.octopus.demo.admin;

import com.octopus.demo.admin.dao.UserDao;
import com.octopus.demo.admin.dao.RoleDao;
import com.octopus.demo.admin.dao.ResourceDao;
import com.octopus.demo.admin.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Testcontainers
@DisplayName("Spring Boot context loads and core beans are available")
class AdminApplicationTest {

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
    private ApplicationContext context;

    @Test
    void contextLoadsAndCoreBeansAreAvailable() {
        assertThat(context).isNotNull();
        assertThat(context.getBean(UserDao.class)).isNotNull();
        assertThat(context.getBean(RoleDao.class)).isNotNull();
        assertThat(context.getBean(ResourceDao.class)).isNotNull();
        assertThat(context.getBean(AuthService.class)).isNotNull();
    }
}