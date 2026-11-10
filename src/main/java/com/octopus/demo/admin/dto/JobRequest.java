package com.octopus.demo.admin.dto;

import com.octopus.demo.admin.entity.SysJob;

/** Body of POST /api/jobs and PUT /api/jobs/{id}. */
public class JobRequest {
    private String jobName;
    private String jobGroup;
    private String beanName;
    private String cron;
    private String params;
    private Integer status;
    private String remark;

    public SysJob toEntity(Long id) {
        SysJob job = new SysJob();
        job.setId(id);
        job.setJobName(trimToNull(jobName));
        job.setJobGroup(jobGroup == null || jobGroup.isBlank() ? "DEFAULT" : jobGroup.trim());
        job.setBeanName(trimToNull(beanName));
        job.setCron(trimToNull(cron));
        job.setParams(trimToNull(params));
        job.setStatus(status);
        job.setRemark(trimToNull(remark));
        return job;
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

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
}
