package com.octopus.demo.admin.dao;

import com.octopus.demo.admin.entity.SysOperLog;
import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;

import java.time.LocalDateTime;

public interface OperLogDao {
    SysOperLog save(SysOperLog log);

    SysOperLog findById(Long id);

    PageResultBean<SysOperLog> findAll(PageQueryBean query, String operator, String module,
                                       Integer status, LocalDateTime from, LocalDateTime to);
}
