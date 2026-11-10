package com.octopus.demo.admin.mapper;

import com.octopus.demo.admin.entity.SysLoginLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface LoginLogMapper {
    int insert(SysLoginLog log);

    List<SysLoginLog> findPage(@Param("username") String username,
                               @Param("status") Integer status,
                               @Param("from") LocalDateTime from,
                               @Param("to") LocalDateTime to,
                               @Param("offset") long offset,
                               @Param("size") int size);

    long count(@Param("username") String username,
               @Param("status") Integer status,
               @Param("from") LocalDateTime from,
               @Param("to") LocalDateTime to);
}
