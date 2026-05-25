package com.octopus.demo.admin.controller;

import com.octopus.demo.admin.entity.SysUser;
import com.octopus.demo.admin.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@DisplayName("AuthController WebMvcTest for Spring Boot 3.x validation")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Test
    @DisplayName("login returns 401 when credentials are invalid")
    void loginReturnsUnauthorizedWhenCredentialsInvalid() throws Exception {
        when(authService.login(anyString(), anyString())).thenReturn(null);

        String body = "{\"username\":\"admin\",\"password\":\"wrong\"}";
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("login returns token and user when credentials are valid")
    void loginReturnsTokenWhenCredentialsValid() throws Exception {
        SysUser user = new SysUser(1L, "admin", "123456");
        when(authService.login("admin", "123456")).thenReturn("test-token-abc");
        when(authService.getCurrentUser("test-token-abc")).thenReturn(user);

        String body = "{\"username\":\"admin\",\"password\":\"123456\"}";
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").value("test-token-abc"))
                .andExpect(jsonPath("$.data.user.username").value("admin"));
    }

    @Test
    @DisplayName("current returns 401 when not logged in")
    void currentReturnsUnauthorizedWhenNotLoggedIn() throws Exception {
        when(authService.getCurrentUser(anyString())).thenReturn(null);

        mockMvc.perform(get("/api/auth/current")
                        .header("X-Token", "invalid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }
}