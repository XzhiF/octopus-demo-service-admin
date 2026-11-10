package com.octopus.demo.admin.dao;

import com.octopus.demo.admin.entity.SysJobLog;
import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;

import java.time.LocalDateTime;

public interface JobLogDao {
    SysJobLog save(SysJobLog log);

    PageResultBean<SysJobLog> findAll(PageQueryBean query, Long jobId, Integer status,
                                      LocalDateTime from, LocalDateTime to);
}
