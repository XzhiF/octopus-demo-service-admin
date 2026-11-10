package com.octopus.demo.admin.service;

import com.octopus.demo.admin.dao.RoleDao;
import com.octopus.demo.admin.dao.UserDao;
import com.octopus.demo.admin.dto.UserSaveRequest;
import com.octopus.demo.admin.entity.SysUser;
import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
import com.octopus.demo.common.util.PasswordUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class UserService {
    private static final long BUILT_IN_ADMIN_ID = 1L;
    private static final String BUILT_IN_ADMIN_USERNAME = "admin";

    private final UserDao userDao;
    private final RoleDao roleDao;

    public UserService(UserDao userDao, RoleDao roleDao) {
        this.userDao = userDao;
        this.roleDao = roleDao;
    }

    @Transactional
    public SysUser create(UserSaveRequest request) {
        String username = requireUsername(request.username());
        requirePassword(request.password());
        if (userDao.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("用户名已存在");
        }
        SysUser user = new SysUser(null, username, PasswordUtils.encode(request.password()));
        user.setNickname(request.nickname());
        user.setGender(normalizeGender(request.gender()));
        user.setStatus(normalizeStatus(request.status()));
        SysUser saved = userDao.save(user);
        if (request.roleIds() != null) {
            replaceRoles(saved.getId(), request.roleIds());
        }
        return userDao.findById(saved.getId()).orElse(saved);
    }

    @Transactional
    public SysUser update(Long id, UserSaveRequest request) {
        SysUser existing = findById(id);
        if (existing == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        String username = requireUsername(request.username());
        userDao.findByUsername(username)
                .filter(other -> !Objects.equals(other.getId(), id))
                .ifPresent(other -> {
                    throw new IllegalArgumentException("用户名已存在");
                });
        existing.setUsername(username);
        existing.setNickname(request.nickname());
        existing.setGender(normalizeGender(request.gender()));
        if (request.status() != null) {
            Integer normalized = normalizeStatus(request.status());
            if (normalized == 0 && isBuiltIn(existing)) {
                throw new IllegalArgumentException("内置管理员不可停用");
            }
            existing.setStatus(normalized);
        }
        String rawPassword = request.password();
        existing.setPassword(rawPassword == null || rawPassword.isBlank()
                ? null : PasswordUtils.encode(rawPassword));
        userDao.save(existing);
        return userDao.findById(id).orElse(existing);
    }

    public void delete(Long id, Long currentUserId) {
        SysUser user = findById(id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (isBuiltIn(user)) {
            throw new IllegalArgumentException("内置管理员不可删除");
        }
        if (Objects.equals(id, currentUserId)) {
            throw new IllegalArgumentException("不能删除自己");
        }
        userDao.deleteById(id);
    }

    public SysUser updateStatus(Long id, Integer status) {
        SysUser user = findById(id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        Integer normalized = normalizeStatus(status);
        if (normalized == 0 && isBuiltIn(user)) {
            throw new IllegalArgumentException("内置管理员不可停用");
        }
        userDao.updateStatus(id, normalized);
        return userDao.findById(id).orElseThrow();
    }

    public void resetPassword(Long id, String rawPassword) {
        SysUser user = findById(id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        requirePassword(rawPassword);
        userDao.updatePassword(id, PasswordUtils.encode(rawPassword));
    }

    public SysUser findById(Long id) {
        return userDao.findById(id).orElse(null);
    }

    public List<SysUser> findAll() {
        return userDao.findAll();
    }

    public PageResultBean<SysUser> findAll(PageQueryBean query, String username, Integer status) {
        return userDao.findAll(query, username, status);
    }

    private void replaceRoles(Long userId, List<Long> roleIds) {
        Set<Long> validIds = new LinkedHashSet<>();
        if (roleIds != null) {
            for (Long roleId : roleIds) {
                if (roleId != null && roleDao.findById(roleId).isPresent()) {
                    validIds.add(roleId);
                }
            }
        }
        userDao.replaceRoles(userId, List.copyOf(validIds));
    }

    private boolean isBuiltIn(SysUser user) {
        return Objects.equals(BUILT_IN_ADMIN_ID, user.getId())
                || BUILT_IN_ADMIN_USERNAME.equals(user.getUsername());
    }

    private String requireUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        return username.trim();
    }

    private void requirePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("密码不能为空");
        }
    }

    /** Blank clears the value; the stored value is the raw {@code sys_user_sex} dict value. */
    private String normalizeGender(String gender) {
        return gender == null || gender.isBlank() ? null : gender.trim();
    }

    private Integer normalizeStatus(Integer status) {
        if (status == null) {
            return 1;
        }
        if (status != 0 && status != 1) {
            throw new IllegalArgumentException("状态只能是 0（停用）或 1（正常）");
        }
        return status;
    }
}
