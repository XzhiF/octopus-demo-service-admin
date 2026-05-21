package com.octopus.demo.admin.dao;

import com.octopus.demo.admin.entity.SysUser;
import java.util.Optional;

public interface UserDao {
    SysUser save(SysUser user);
    Optional<SysUser> findById(Long id);
    Optional<SysUser> findByUsername(String username);
    java.util.List<SysUser> findAll();
    void deleteById(Long id);
}