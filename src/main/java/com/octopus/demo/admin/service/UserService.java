package com.octopus.demo.admin.service;

import com.octopus.demo.admin.dao.UserDao;
import com.octopus.demo.admin.dao.RoleDao;
import com.octopus.demo.admin.entity.SysUser;
import com.octopus.demo.admin.entity.SysRole;
import com.octopus.demo.common.audit.AuditContext;
import com.octopus.demo.common.audit.AuditEvent;
import com.octopus.demo.common.audit.AuditLogger;
import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserDao userDao;
    private final RoleDao roleDao;
    private final AuditLogger auditLogger;

    public UserService(UserDao userDao, RoleDao roleDao, AuditLogger auditLogger) {
        this.userDao = userDao;
        this.roleDao = roleDao;
        this.auditLogger = auditLogger;
    }

    public SysUser create(SysUser user) {
        SysUser created = userDao.save(user);
        safeAudit(() -> auditLogger.log(AuditEvent.of(
            AuditContext.getCurrentUserId(),
            "CREATE", "USER", String.valueOf(created.getId()))));
        return created;
    }

    public SysUser update(SysUser user) {
        SysUser updated = userDao.save(user);
        safeAudit(() -> auditLogger.log(AuditEvent.of(
            AuditContext.getCurrentUserId(),
            "UPDATE", "USER", String.valueOf(updated.getId()))));
        return updated;
    }

    public void delete(Long id) {
        // 先检查存在性，避免为不存在的用户产生虚假 DELETE 审计记录。
        // 注意：存在 TOCTOU 竞态（检查后另一线程可能删除），Demo 项目可接受。
        boolean exists = userDao.findById(id).isPresent();
        userDao.deleteById(id);
        if (exists) {
            safeAudit(() -> auditLogger.log(AuditEvent.of(
                AuditContext.getCurrentUserId(),
                "DELETE", "USER", String.valueOf(id))));
        }
    }

    public SysUser findById(Long id) {
        return userDao.findById(id).orElse(null);
    }

    public List<SysUser> findAll() {
        return userDao.findAll();
    }

    public PageResultBean<SysUser> findAll(PageQueryBean query) {
        return userDao.findAll(query);
    }

    public SysUser assignRoles(Long userId, List<Long> roleIds) {
        SysUser user = userDao.findById(userId).orElse(null);
        if (user == null) return null;
        List<SysRole> roles = roleIds.stream()
            .map(roleDao::findById)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
        user.setRoles(roles);
        SysUser saved = userDao.save(user);
        safeAudit(() -> auditLogger.log(AuditEvent.of(
            AuditContext.getCurrentUserId(),
            "ASSIGN_ROLE", "USER", String.valueOf(userId),
            Map.of("roleIds", roleIds.toString()))));
        return saved;
    }

    private void safeAudit(Runnable auditAction) {
        try {
            auditAction.run();
        } catch (Exception e) {
            log.warn("审计日志记录失败，不影响业务操作: {}", e.getMessage(), e);
        }
    }
}
