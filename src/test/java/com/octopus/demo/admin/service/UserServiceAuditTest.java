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
}
