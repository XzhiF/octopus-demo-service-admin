package com.octopus.demo.admin.entity;

import java.time.LocalDateTime;

/** A row of {@code sys_oper_log}: one annotated write operation. */
public class SysOperLog {
    private Long id;
    private String title;
    private String module;
    private String httpMethod;
    private String requestUri;
    private String params;
    private String operator;
    private String ip;
    private Integer status;
    private String errorMsg;
    private Long durationMs;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }

    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }

    public String getRequestUri() { return requestUri; }
    public void setRequestUri(String requestUri) { this.requestUri = requestUri; }

    public String getParams() { return params; }
    public void setParams(String params) { this.params = params; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }

    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
