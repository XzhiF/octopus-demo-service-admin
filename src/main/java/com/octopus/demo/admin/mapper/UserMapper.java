package com.octopus.demo.admin.mapper;

import com.octopus.demo.admin.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {
    SysUser findById(@Param("id") Long id);

    SysUser findByUsername(@Param("username") String username);

    List<SysUser> findAll();

    List<SysUser> findPage(@Param("offset") long offset, @Param("size") int size);

    long count();

    int insert(SysUser user);

    int update(SysUser user);

    int deleteById(@Param("id") Long id);

    int deleteUserRoles(@Param("userId") Long userId);

    int insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);
}
