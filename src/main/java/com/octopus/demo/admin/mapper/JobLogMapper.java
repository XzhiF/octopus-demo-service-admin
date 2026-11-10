package com.octopus.demo.admin.mapper;

import com.octopus.demo.admin.entity.SysJobLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface JobLogMapper {
    int insert(SysJobLog log);

    List<SysJobLog> findPage(@Param("jobId") Long jobId,
                             @Param("status") Integer status,
                             @Param("from") LocalDateTime from,
                             @Param("to") LocalDateTime to,
                             @Param("offset") long offset,
                             @Param("size") int size);

    long count(@Param("jobId") Long jobId,
               @Param("status") Integer status,
               @Param("from") LocalDateTime from,
               @Param("to") LocalDateTime to);
}
