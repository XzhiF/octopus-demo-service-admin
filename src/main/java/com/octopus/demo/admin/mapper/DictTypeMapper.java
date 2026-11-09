package com.octopus.demo.admin.mapper;

import com.octopus.demo.admin.entity.SysDictType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DictTypeMapper {
    SysDictType findById(@Param("id") Long id);

    SysDictType findByType(@Param("dictType") String dictType);

    List<SysDictType> findPage(@Param("offset") long offset,
                               @Param("size") int size,
                               @Param("dictType") String dictType,
                               @Param("dictName") String dictName);

    long count(@Param("dictType") String dictType, @Param("dictName") String dictName);

    int insert(SysDictType type);

    int update(SysDictType type);

    int deleteById(@Param("id") Long id);
}
