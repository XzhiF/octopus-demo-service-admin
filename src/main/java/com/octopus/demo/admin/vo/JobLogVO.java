package com.octopus.demo.admin.vo;

import com.octopus.demo.admin.entity.SysJobLog;

import java.time.LocalDateTime;

/** Mirrors a sys_job_log row for the execution log drawer. */
public record JobLogVO(Long id, Long jobId, String jobName, Integer status, String message,
                       LocalDateTime startedAt, LocalDateTime finishedAt, Long durationMs,
                       String triggerType) {

    public static JobLogVO from(SysJobLog log) {
        return new JobLogVO(
                log.getId(),
                log.getJobId(),
                log.getJobName(),
                log.getStatus(),
                log.getMessage(),
                log.getStartedAt(),
                log.getFinishedAt(),
                log.getDurationMs(),
                log.getTriggerType());
    }
}
