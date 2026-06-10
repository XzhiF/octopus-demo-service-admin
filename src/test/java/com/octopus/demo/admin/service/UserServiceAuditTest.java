package com.octopus.demo.admin.service;

import com.octopus.demo.admin.dao.RoleDao;
import com.octopus.demo.admin.dao.UserDao;
import com.octopus.demo.admin.entity.SysUser;
import com.octopus.demo.common.audit.AuditEvent;
import com.octopus.demo.common.audit.AuditLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceAuditTest {

    @Mock private UserDao userDao;
    @Mock private RoleDao roleDao;
    @Mock private AuditLogger auditLogger;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userDao, roleDao, auditLogger);
    }

    @Test
    @DisplayName("create logs CREATE audit event")
    void create_logsAuditEvent() {
        var user = new SysUser();
        user.setId(1L);
        when(userDao.save(any(SysUser.class))).thenReturn(user);

        userService.create(user);

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditLogger).log(captor.capture());
        assertEquals("CREATE", captor.getValue().action());
    }

    @Test
    @DisplayName("delete logs DELETE only when user exists")
    void delete_logsOnlyWhenExists() {
        when(userDao.findById(1L)).thenReturn(Optional.of(new SysUser()));

        userService.delete(1L);

        verify(auditLogger).log(any(AuditEvent.class));
    }

    @Test
    @DisplayName("delete does not log when user not found")
    void delete_noAuditWhenNotFound() {
        when(userDao.findById(999L)).thenReturn(Optional.empty());

        userService.delete(999L);

        verify(auditLogger, never()).log(any());
    }

    @Test
    @DisplayName("create succeeds even when audit logging fails")
    void create_auditFails_businessSucceeds() {
        var user = new SysUser();
        user.setId(1L);
        when(userDao.save(any(SysUser.class))).thenReturn(user);
        doThrow(new RuntimeException("audit unavailable")).when(auditLogger).log(any(AuditEvent.class));

        SysUser created = userService.create(user);

        assertNotNull(created);
        verify(userDao).save(any(SysUser.class));
    }

    @Test
    @DisplayName("assignRoles logs ASSIGN_ROLE audit event with comma-separated roleIds")
    void assignRoles_logsAuditEvent() {
        var user = new SysUser();
        user.setId(1L);
        var role1 = new com.octopus.demo.admin.entity.SysRole();
        role1.setId(10L);
        var role2 = new com.octopus.demo.admin.entity.SysRole();
        role2.setId(20L);
        when(userDao.findById(1L)).thenReturn(Optional.of(user));
        when(roleDao.findById(10L)).thenReturn(Optional.of(role1));
        when(roleDao.findById(20L)).thenReturn(Optional.of(role2));
        when(userDao.save(any(SysUser.class))).thenReturn(user);

        userService.assignRoles(1L, List.of(10L, 20L));

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditLogger).log(captor.capture());
        assertEquals("ASSIGN_ROLE", captor.getValue().action());
        assertEquals("10,20", captor.getValue().details().get("roleIds"));
    }
}
