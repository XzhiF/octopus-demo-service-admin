package com.octopus.demo.admin.schedule;

import com.octopus.demo.admin.dao.JobDao;
import com.octopus.demo.admin.entity.SysJob;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Keeps the Quartz scheduler in step with {@code sys_job}: registers enabled rows on startup
 * and incrementally on create/update/delete.
 */
@Service
public class JobSyncService {

    private static final Logger log = LoggerFactory.getLogger(JobSyncService.class);
    static final int STATUS_ENABLED = 1;

    private final JobDao jobDao;
    private final Scheduler scheduler;

    public JobSyncService(JobDao jobDao, Scheduler scheduler) {
        this.jobDao = jobDao;
        this.scheduler = scheduler;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        syncAll();
    }

    /** Registers every enabled row; called on startup and usable to re-register after a reset. */
    public void syncAll() {
        for (SysJob job : jobDao.findAllEnabled()) {
            try {
                schedule(job);
            } catch (SchedulerException ex) {
                log.error("Failed to register job {} ({})", job.getJobName(), job.getJobGroup(), ex);
            }
        }
    }

    /** Applies the row's current status: enabled rows are scheduled, paused rows unscheduled. */
    public void sync(SysJob job) {
        try {
            if (job.getStatus() != null && job.getStatus() == STATUS_ENABLED) {
                schedule(job);
            } else {
                unschedule(job);
            }
        } catch (SchedulerException ex) {
            throw new IllegalStateException("同步调度器失败：" + job.getJobName(), ex);
        }
    }

    public void schedule(SysJob job) throws SchedulerException {
        JobKey jobKey = jobKey(job);
        // Replacing the job also removes its previous triggers, so a cron change takes effect.
        if (scheduler.checkExists(jobKey)) {
            scheduler.deleteJob(jobKey);
        }
        JobDetail detail = JobBuilder.newJob(QuartzJob.class)
                .withIdentity(jobKey)
                .usingJobData("jobId", job.getId() == null ? 0L : job.getId())
                .usingJobData("jobName", job.getJobName())
                .usingJobData("beanName", job.getBeanName())
                .usingJobData("params", job.getParams() == null ? "" : job.getParams())
                .build();
        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(new TriggerKey(job.getJobName(), job.getJobGroup()))
                .forJob(detail)
                .withSchedule(CronScheduleBuilder.cronSchedule(job.getCron())
                        .withMisfireHandlingInstructionDoNothing())
                .build();
        scheduler.scheduleJob(detail, trigger);
    }

    /** Pauses the job's triggers so no cron fire happens while the row is parked in status 0. */
    public void pause(SysJob job) {
        try {
            JobKey jobKey = jobKey(job);
            if (scheduler.checkExists(jobKey)) {
                scheduler.pauseJob(jobKey);
            }
        } catch (SchedulerException ex) {
            throw new IllegalStateException("暂停调度器任务失败：" + job.getJobName(), ex);
        }
    }

    /** Resumes paused triggers, or registers the job when it was never scheduled (created disabled). */
    public void resume(SysJob job) {
        try {
            JobKey jobKey = jobKey(job);
            if (scheduler.checkExists(jobKey)) {
                scheduler.resumeJob(jobKey);
            } else {
                schedule(job);
            }
        } catch (SchedulerException ex) {
            throw new IllegalStateException("恢复调度器任务失败：" + job.getJobName(), ex);
        }
    }

    /**
     * Fires the job once without touching its cron or status. The trigger data map marks the fire
     * as manual so {@link QuartzJob} records {@code trigger_type=manual}.
     */
    public void triggerNow(SysJob job) {
        try {
            JobKey jobKey = jobKey(job);
            if (!scheduler.checkExists(jobKey)) {
                schedule(job);
                if (job.getStatus() == null || job.getStatus() != STATUS_ENABLED) {
                    // Park the freshly registered cron trigger; only the manual fire should run.
                    scheduler.pauseJob(jobKey);
                }
            }
            JobDataMap data = new JobDataMap();
            data.put("triggerType", QuartzJob.TRIGGER_TYPE_MANUAL);
            scheduler.triggerJob(jobKey, data);
        } catch (SchedulerException ex) {
            throw new IllegalStateException("立即执行调度器任务失败：" + job.getJobName(), ex);
        }
    }

    /** Removes the job and its triggers from Quartz; safe when the job was never registered. */
    public void remove(String jobName, String jobGroup) {
        try {
            JobKey jobKey = new JobKey(jobName, jobGroup);
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
            }
        } catch (SchedulerException ex) {
            throw new IllegalStateException("移除调度器任务失败：" + jobName, ex);
        }
    }

    public void unschedule(SysJob job) throws SchedulerException {
        JobKey jobKey = jobKey(job);
        if (scheduler.checkExists(jobKey)) {
            scheduler.deleteJob(jobKey);
        }
    }

    private JobKey jobKey(SysJob job) {
        return new JobKey(job.getJobName(), job.getJobGroup());
    }
}
