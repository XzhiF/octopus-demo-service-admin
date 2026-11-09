package com.octopus.demo.admin.mapper;

import com.octopus.demo.admin.entity.SysDictData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DictDataMapper {
    SysDictData findById(@Param("id") Long id);

    List<SysDictData> findByType(@Param("dictType") String dictType);

    List<SysDictData> findEnabledByType(@Param("dictType") String dictType);

    int insert(SysDictData data);

    int update(SysDictData data);

    int deleteById(@Param("id") Long id);

    long countByType(@Param("dictType") String dictType);
}
