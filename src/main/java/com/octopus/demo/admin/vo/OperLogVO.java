package com.octopus.demo.admin.vo;

import com.octopus.demo.admin.entity.SysOperLog;

import java.time.LocalDateTime;

/** Mirrors a sys_oper_log row for the operation log table and detail drawer. */
public record OperLogVO(Long id, String title, String module, String httpMethod, String requestUri,
                        String params, String operator, String ip, Integer status, String errorMsg,
                        Long durationMs, LocalDateTime createdAt) {

    public static OperLogVO from(SysOperLog log) {
        return new OperLogVO(
                log.getId(),
                log.getTitle(),
                log.getModule(),
                log.getHttpMethod(),
                log.getRequestUri(),
                log.getParams(),
                log.getOperator(),
                log.getIp(),
                log.getStatus(),
                log.getErrorMsg(),
                log.getDurationMs(),
                log.getCreatedAt());
    }
}
