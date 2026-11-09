package com.octopus.demo.admin.service;

import com.octopus.demo.admin.dao.RoleDao;
import com.octopus.demo.admin.entity.SysRole;
import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class RoleService {
    private static final String BUILT_IN_CODE = "admin";
    private static final Pattern CODE_PATTERN = Pattern.compile("^[a-z0-9_]+$");

    private final RoleDao roleDao;

    public RoleService(RoleDao roleDao) {
        this.roleDao = roleDao;
    }

    public SysRole create(SysRole role) {
        validate(role, null);
        validateStatus(role.getStatus());
        if (role.getStatus() == null) {
            role.setStatus(1);
        }
        return roleDao.save(role);
    }

    public SysRole update(Long id, SysRole role) {
        SysRole existing = requireRole(id);
        if (BUILT_IN_CODE.equals(existing.getCode())) {
            if (role.getCode() != null && !BUILT_IN_CODE.equals(role.getCode())) {
                throw new IllegalArgumentException("内置角色编码不可修改");
            }
            role.setCode(BUILT_IN_CODE);
        }
        validate(role, id);
        validateStatus(role.getStatus());
        if (role.getStatus() != null && role.getStatus() == 0
                && BUILT_IN_CODE.equals(existing.getCode())) {
            throw new IllegalArgumentException("内置角色不可停用");
        }
        role.setId(id);
        return roleDao.save(role);
    }

    public void delete(Long id) {
        SysRole role = requireRole(id);
        if (BUILT_IN_CODE.equals(role.getCode())) {
            throw new IllegalArgumentException("内置角色不可删除");
        }
        long users = roleDao.countUsers(id);
        if (users > 0) {
            throw new IllegalArgumentException("该角色已被 " + users + " 个用户占用，无法删除");
        }
        roleDao.deleteById(id);
    }

    public SysRole updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new IllegalArgumentException("状态值只能为 0 或 1");
        }
        SysRole role = requireRole(id);
        if (BUILT_IN_CODE.equals(role.getCode()) && status == 0) {
            throw new IllegalArgumentException("内置角色不可停用");
        }
        roleDao.updateStatus(id, status);
        return roleDao.findById(id).orElseThrow();
    }

    public SysRole findById(Long id) {
        return roleDao.findById(id).orElse(null);
    }

    public List<SysRole> findAll() {
        return roleDao.findAll();
    }

    public PageResultBean<SysRole> findAll(PageQueryBean query, String name, Integer status) {
        return roleDao.findAll(query, name, status);
    }

    public List<Long> findMenuIds(Long roleId) {
        requireRole(roleId);
        return roleDao.findMenuIds(roleId);
    }

    private void validateStatus(Integer status) {
        if (status != null && status != 0 && status != 1) {
            throw new IllegalArgumentException("状态值只能为 0 或 1");
        }
    }

    private SysRole requireRole(Long id) {
        return roleDao.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("角色不存在"));
    }

    private void validate(SysRole role, Long selfId) {
        if (role.getName() == null || role.getName().isBlank()) {
            throw new IllegalArgumentException("角色名称不能为空");
        }
        String code = role.getCode();
        if (code == null || !CODE_PATTERN.matcher(code).matches()) {
            throw new IllegalArgumentException("角色编码只能包含小写字母、数字和下划线");
        }
        roleDao.findByCode(code)
                .filter(other -> selfId == null || !selfId.equals(other.getId()))
                .ifPresent(other -> {
                    throw new IllegalArgumentException("角色编码已存在");
                });
    }
}
