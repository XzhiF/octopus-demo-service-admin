package com.octopus.demo.admin.mapper;

import com.octopus.demo.admin.entity.SysOperLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OperLogMapper {

    int insert(SysOperLog log);

    SysOperLog findById(@Param("id") Long id);

    List<SysOperLog> findPage(@Param("operator") String operator,
                              @Param("module") String module,
                              @Param("status") Integer status,
                              @Param("from") LocalDateTime from,
                              @Param("to") LocalDateTime to,
                              @Param("offset") long offset,
                              @Param("size") int size);

    long count(@Param("operator") String operator,
               @Param("module") String module,
               @Param("status") Integer status,
               @Param("from") LocalDateTime from,
               @Param("to") LocalDateTime to);
}
