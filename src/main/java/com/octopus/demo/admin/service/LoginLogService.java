package com.octopus.demo.admin.service;

import com.octopus.demo.admin.dao.LoginLogDao;
import com.octopus.demo.admin.entity.SysLoginLog;
import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LoginLogService {

    private final LoginLogDao loginLogDao;

    public LoginLogService(LoginLogDao loginLogDao) {
        this.loginLogDao = loginLogDao;
    }

    public SysLoginLog save(SysLoginLog log) {
        log.setUsername(truncate(log.getUsername(), 64));
        log.setIp(truncate(log.getIp(), 128));
        log.setUserAgent(truncate(log.getUserAgent(), 512));
        log.setMessage(truncate(log.getMessage(), 255));
        return loginLogDao.save(log);
    }

    public PageResultBean<SysLoginLog> findAll(PageQueryBean query, String username, Integer status,
                                               LocalDateTime from, LocalDateTime to) {
        return loginLogDao.findAll(query, username, status, from, to);
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
