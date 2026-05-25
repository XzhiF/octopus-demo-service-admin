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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("Spring Boot context loads and core beans are available")
class AdminApplicationTest {

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