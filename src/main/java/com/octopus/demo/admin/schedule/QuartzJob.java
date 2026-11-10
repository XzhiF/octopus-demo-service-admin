package com.octopus.demo.admin.schedule;

import com.octopus.demo.admin.dao.JobLogDao;
import com.octopus.demo.admin.entity.SysJobLog;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/** Generic Quartz job that resolves the {@link ScheduledTask} bean named in the job data map. */
@DisallowConcurrentExecution
public class QuartzJob implements Job {

    static final int STATUS_SUCCESS = 1;
    static final int STATUS_FAILURE = 0;
    static final int MESSAGE_MAX_LENGTH = 500;
    static final String TRIGGER_TYPE_MANUAL = "manual";
    static final String TRIGGER_TYPE_CRON = "cron";

    private ApplicationContext applicationContext;
    private JobLogDao jobLogDao;

    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Autowired
    public void setJobLogDao(JobLogDao jobLogDao) {
        this.jobLogDao = jobLogDao;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap data = context.getMergedJobDataMap();
        String beanName = data.getString("beanName");
        if (beanName == null || beanName.isBlank()) {
            throw new JobExecutionException("sys_job row has no bean_name");
        }
        Long jobId = data.containsKey("jobId") ? data.getLong("jobId") : null;
        String jobName = data.getString("jobName");
        String triggerType = data.containsKey("triggerType") ? data.getString("triggerType") : TRIGGER_TYPE_CRON;

        LocalDateTime startedAt = LocalDateTime.now();
        long startNanos = System.nanoTime();
        try {
            ScheduledTask task = applicationContext.getBean(beanName, ScheduledTask.class);
            String result = task.execute(data.getString("params"));
            String message = (result == null || result.isBlank()) ? "success" : truncate(result);
            recordLog(jobId, jobName, STATUS_SUCCESS, message, startedAt, startNanos, triggerType);
        } catch (Exception ex) {
            String reason = ex.getMessage() == null ? ex.getClass().getName() : ex.getMessage();
            recordLog(jobId, jobName, STATUS_FAILURE, truncate(reason), startedAt, startNanos, triggerType);
            throw ex instanceof JobExecutionException ? (JobExecutionException) ex : new JobExecutionException(ex);
        }
    }

    private void recordLog(Long jobId, String jobName, int status, String message,
                           LocalDateTime startedAt, long startNanos, String triggerType) {
        if (jobLogDao == null) {
            return;
        }
        SysJobLog log = new SysJobLog();
        log.setJobId(jobId);
        log.setJobName(jobName);
        log.setStatus(status);
        log.setMessage(message);
        log.setStartedAt(startedAt);
        log.setFinishedAt(LocalDateTime.now());
        log.setDurationMs(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
        log.setTriggerType(triggerType == null ? TRIGGER_TYPE_CRON : triggerType);
        jobLogDao.save(log);
    }

    static String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= MESSAGE_MAX_LENGTH ? message : message.substring(0, MESSAGE_MAX_LENGTH);
    }
}
