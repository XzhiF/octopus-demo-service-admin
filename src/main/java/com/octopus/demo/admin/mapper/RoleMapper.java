package com.octopus.demo.admin.mapper;

import com.octopus.demo.admin.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RoleMapper {
    SysRole findById(@Param("id") Long id);

    SysRole findByCode(@Param("code") String code);

    List<SysRole> findAll();

    List<SysRole> findPage(@Param("offset") long offset,
                           @Param("size") int size,
                           @Param("name") String name,
                           @Param("status") Integer status);

    long count(@Param("name") String name, @Param("status") Integer status);

    int insert(SysRole role);

    int update(SysRole role);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    int deleteById(@Param("id") Long id);

    int deleteRoleMenus(@Param("roleId") Long roleId);

    int insertRoleMenu(@Param("roleId") Long roleId, @Param("menuId") Long menuId);

    List<Long> findMenuIds(@Param("roleId") Long roleId);

    long countUsers(@Param("roleId") Long roleId);
}
