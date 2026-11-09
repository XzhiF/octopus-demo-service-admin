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

    List<SysRole> findPage(@Param("offset") long offset, @Param("size") int size);

    long count();

    int insert(SysRole role);

    int update(SysRole role);

    int deleteById(@Param("id") Long id);

    int deleteRoleMenus(@Param("roleId") Long roleId);

    int insertRoleMenu(@Param("roleId") Long roleId, @Param("menuId") Long menuId);
}
