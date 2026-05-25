package com.octopus.demo.admin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AdminApplicationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoadsAndCoreBeansAreAvailable() {
        assertThat(context).isNotNull();
        assertThat(context.getBean("userDao")).isNotNull();
        assertThat(context.getBean("roleDao")).isNotNull();
        assertThat(context.getBean("resourceDao")).isNotNull();
        assertThat(context.getBean("authService")).isNotNull();
    }
}