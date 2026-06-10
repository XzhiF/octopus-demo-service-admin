package com.octopus.demo.admin.controller;

import com.octopus.demo.common.audit.AuditEvent;
import com.octopus.demo.common.audit.AuditLogger;
import com.octopus.demo.common.audit.AuditQuery;
import com.octopus.demo.common.auth.AuthAutoConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuditLogController.class)
@Import(AuthAutoConfiguration.class)
class AuditLogControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private AuditLogger auditLogger;

    @Test
    @DisplayName("GET /api/admin/audit-logs returns events when authenticated")
    void getAuditLogs_returnsEvents() throws Exception {
        var event = new AuditEvent(Instant.now(), 1L, "ASSIGN_ROLE", "USER", "1", Map.of());
        when(auditLogger.query(any(AuditQuery.class))).thenReturn(List.of(event));

        mockMvc.perform(get("/api/admin/audit-logs").header("X-User-Id", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data[0].action").value("ASSIGN_ROLE"));
    }

    @Test
    @DisplayName("GET /api/admin/audit-logs with entityType filter")
    void getAuditLogs_withEntityType() throws Exception {
        when(auditLogger.query(any(AuditQuery.class))).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/audit-logs")
                .header("X-User-Id", "1")
                .param("entityType", "ROLE")
                .param("limit", "50"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/admin/audit-logs returns 401 when X-User-Id header is missing")
    void getAuditLogs_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/audit-logs"))
            .andExpect(status().isUnauthorized());
    }
}
