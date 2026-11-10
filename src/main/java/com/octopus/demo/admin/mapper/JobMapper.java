package com.octopus.demo.admin.mapper;

import com.octopus.demo.admin.entity.SysJob;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface JobMapper {
    SysJob findById(@Param("id") Long id);

    List<SysJob> findPage(@Param("jobName") String jobName,
                          @Param("status") Integer status,
                          @Param("offset") long offset,
                          @Param("size") int size);

    long count(@Param("jobName") String jobName, @Param("status") Integer status);

    List<SysJob> findAllEnabled();

    int insert(SysJob job);

    int update(SysJob job);

    int deleteById(@Param("id") Long id);

    /** (job_name, job_group) uniqueness check; {@code excludeId} skips the row being updated. */
    int countByNameAndGroup(@Param("jobName") String jobName,
                            @Param("jobGroup") String jobGroup,
                            @Param("excludeId") Long excludeId);
}
