package com.octopus.demo.admin.dao;

import com.octopus.demo.admin.entity.SysLoginLog;
import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;

import java.time.LocalDateTime;

public interface LoginLogDao {
    SysLoginLog save(SysLoginLog log);

    PageResultBean<SysLoginLog> findAll(PageQueryBean query, String username, Integer status,
                                        LocalDateTime from, LocalDateTime to);
}
