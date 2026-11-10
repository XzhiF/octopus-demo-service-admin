package com.octopus.demo.admin.vo;

import com.octopus.demo.admin.entity.SysJob;

import java.time.LocalDateTime;

/** Mirrors a sys_job row for the admin list page. */
public record JobVO(Long id, String jobName, String jobGroup, String beanName, String cron,
                    String params, Integer status, String remark,
                    LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime lastRunAt) {

    public static JobVO from(SysJob job) {
        return new JobVO(
                job.getId(),
                job.getJobName(),
                job.getJobGroup(),
                job.getBeanName(),
                job.getCron(),
                job.getParams(),
                job.getStatus(),
                job.getRemark(),
                job.getCreatedAt(),
                job.getUpdatedAt(),
                job.getLastRunAt());
    }
}
