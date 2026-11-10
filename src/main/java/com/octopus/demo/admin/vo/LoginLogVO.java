package com.octopus.demo.admin.vo;

import com.octopus.demo.admin.entity.SysLoginLog;

import java.time.LocalDateTime;

/** Mirrors a sys_login_log row for the login log page. */
public record LoginLogVO(Long id, String username, String ip, String userAgent, Integer status,
                         String message, LocalDateTime loginAt) {

    public static LoginLogVO from(SysLoginLog log) {
        return new LoginLogVO(
                log.getId(),
                log.getUsername(),
                log.getIp(),
                log.getUserAgent(),
                log.getStatus(),
                log.getMessage(),
                log.getLoginAt());
    }
}
