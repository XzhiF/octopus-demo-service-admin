package com.octopus.demo.admin.entity;

import java.time.LocalDateTime;

/** A row of {@code sys_job}: a scheduled task definition. */
public class SysJob {
    private Long id;
    private String jobName;
    private String jobGroup;
    private String beanName;
    private String cron;
    private String params;
    private Integer status;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /** Latest sys_job_log.started_at for this job; null until it has run. */
    private LocalDateTime lastRunAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getJobName() { return jobName; }
    public void setJobName(String jobName) { this.jobName = jobName; }

    public String getJobGroup() { return jobGroup; }
    public void setJobGroup(String jobGroup) { this.jobGroup = jobGroup; }

    public String getBeanName() { return beanName; }
    public void setBeanName(String beanName) { this.beanName = beanName; }

    public String getCron() { return cron; }
    public void setCron(String cron) { this.cron = cron; }

    public String getParams() { return params; }
    public void setParams(String params) { this.params = params; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getLastRunAt() { return lastRunAt; }
    public void setLastRunAt(LocalDateTime lastRunAt) { this.lastRunAt = lastRunAt; }
}
