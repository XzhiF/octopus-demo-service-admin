package com.octopus.demo.admin.dao;

import com.octopus.demo.admin.entity.SysJob;
import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;

import java.util.List;
import java.util.Optional;

public interface JobDao {
    SysJob save(SysJob job);

    Optional<SysJob> findById(Long id);

    PageResultBean<SysJob> findAll(PageQueryBean query, String jobName, Integer status);

    List<SysJob> findAllEnabled();

    void deleteById(Long id);

    int countByNameAndGroup(String jobName, String jobGroup, Long excludeId);
}
