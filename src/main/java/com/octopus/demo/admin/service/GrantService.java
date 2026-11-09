package com.octopus.demo.admin.service;

import com.octopus.demo.admin.dao.ResourceDao;
import com.octopus.demo.admin.dao.RoleDao;
import com.octopus.demo.admin.dao.UserDao;
import com.octopus.demo.admin.entity.SysRole;
import com.octopus.demo.admin.entity.SysUser;
import com.octopus.demo.admin.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Single place for the two grant relations: PUT is a full replace inside one transaction,
 * so re-submitting the same list is idempotent and clearing with an empty list removes every row.
 * Permissions are loaded from these tables on every request, so the next request sees the change.
 */
@Service
public class GrantService {

    private final UserDao userDao;
    private final RoleDao roleDao;
    private final ResourceDao resourceDao;

    public GrantService(UserDao userDao, RoleDao roleDao, ResourceDao resourceDao) {
        this.userDao = userDao;
        this.roleDao = roleDao;
        this.resourceDao = resourceDao;
    }

    @Transactional
    public SysUser replaceUserRoles(Long userId, List<Long> roleIds) {
        if (userDao.findById(userId).isEmpty()) {
            throw new NotFoundException("用户不存在");
        }
        Set<Long> ids = normalize(roleIds);
        for (Long roleId : ids) {
            if (roleDao.findById(roleId).isEmpty()) {
                throw new IllegalArgumentException("角色不存在: " + roleId);
            }
        }
        userDao.replaceRoles(userId, List.copyOf(ids));
        return userDao.findById(userId).orElseThrow();
    }

    @Transactional
    public SysRole replaceRoleMenus(Long roleId, List<Long> menuIds) {
        if (roleDao.findById(roleId).isEmpty()) {
            throw new NotFoundException("角色不存在");
        }
        Set<Long> ids = normalize(menuIds);
        for (Long menuId : ids) {
            if (resourceDao.findById(menuId).isEmpty()) {
                throw new IllegalArgumentException("菜单不存在: " + menuId);
            }
        }
        roleDao.replaceMenus(roleId, List.copyOf(ids));
        return roleDao.findById(roleId).orElseThrow();
    }

    private Set<Long> normalize(List<Long> ids) {
        Set<Long> result = new LinkedHashSet<>();
        if (ids == null) {
            return result;
        }
        for (Long id : ids) {
            if (id == null) {
                throw new IllegalArgumentException("id 列表不能包含空值");
            }
            result.add(id);
        }
        return result;
    }
}
