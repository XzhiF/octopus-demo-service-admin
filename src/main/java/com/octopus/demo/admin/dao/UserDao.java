package com.octopus.demo.admin.dao;

import com.octopus.demo.admin.entity.SysUser;
import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
import java.util.List;
import java.util.Optional;

public interface UserDao {
    SysUser save(SysUser user);
    Optional<SysUser> findById(Long id);
    Optional<SysUser> findByUsername(String username);
    List<SysUser> findAll();
    void deleteById(Long id);
    PageResultBean<SysUser> findAll(PageQueryBean query, String username, Integer status);
    void updateStatus(Long id, Integer status);
    void updatePassword(Long id, String passwordHash);
    void replaceRoles(Long userId, List<Long> roleIds);
}
