package com.octopus.demo.admin.mapper;

import com.octopus.demo.admin.entity.SysResource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MenuMapper {
    SysResource findById(@Param("id") Long id);

    SysResource findByCode(@Param("code") String code);

    List<SysResource> findAll();

    List<SysResource> findPage(@Param("offset") long offset, @Param("size") int size);

    long count();

    int insert(SysResource menu);

    int update(SysResource menu);

    int deleteById(@Param("id") Long id);

    /** Visible catalog/menu nodes (no buttons) granted to the user, ordered by sort. */
    List<SysResource> findVisibleByUserId(@Param("userId") Long userId);
}
