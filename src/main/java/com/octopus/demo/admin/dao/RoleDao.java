package com.octopus.demo.admin.dao;

import com.octopus.demo.admin.entity.SysRole;
import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
import java.util.Optional;

public interface RoleDao {
    SysRole save(SysRole role);
    Optional<SysRole> findById(Long id);
    Optional<SysRole> findByCode(String code);
    java.util.List<SysRole> findAll();
    void deleteById(Long id);
    PageResultBean<SysRole> findAll(PageQueryBean query, String name, Integer status);
    void updateStatus(Long id, Integer status);

    /** Full replace: drops every existing grant then inserts the given menu ids. */
    void replaceMenus(Long roleId, java.util.List<Long> menuIds);

    java.util.List<Long> findMenuIds(Long roleId);

    long countUsers(Long roleId);
}
