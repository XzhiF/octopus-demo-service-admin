package com.octopus.demo.admin.service;

import com.octopus.demo.admin.dao.JobDao;
import com.octopus.demo.admin.dto.JobRequest;
import com.octopus.demo.admin.entity.SysJob;
import com.octopus.demo.admin.exception.NotFoundException;
import com.octopus.demo.admin.schedule.JobSyncService;
import com.octopus.demo.admin.schedule.ScheduledTask;
import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
import org.quartz.CronExpression;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.Objects;

@Service
public class JobService {

    private final JobDao jobDao;
    private final JobSyncService jobSyncService;
    private final ApplicationContext applicationContext;

    public JobService(JobDao jobDao, JobSyncService jobSyncService, ApplicationContext applicationContext) {
        this.jobDao = jobDao;
        this.jobSyncService = jobSyncService;
        this.applicationContext = applicationContext;
    }

    public PageResultBean<SysJob> findAll(PageQueryBean query, String jobName, Integer status) {
        return jobDao.findAll(query, jobName == null ? null : jobName.trim(), status);
    }

    public SysJob create(JobRequest request) {
        SysJob job = request.toEntity(null);
        if (job.getStatus() == null) {
            job.setStatus(1);
        }
        validate(job);
        ensureNameUnique(job.getJobName(), job.getJobGroup(), null);
        SysJob saved = jobDao.save(job);
        jobSyncService.sync(saved);
        return saved;
    }

    public SysJob update(Long id, JobRequest request) {
        SysJob existing = jobDao.findById(id).orElseThrow(() -> new NotFoundException("任务不存在"));
        SysJob job = request.toEntity(id);
        if (job.getStatus() == null) {
            job.setStatus(existing.getStatus());
        }
        validate(job);
        ensureNameUnique(job.getJobName(), job.getJobGroup(), id);
        SysJob saved = jobDao.save(job);
        // A rename moves the Quartz key; drop the old entry or it keeps firing with stale data.
        if (!Objects.equals(existing.getJobName(), saved.getJobName())
                || !Objects.equals(existing.getJobGroup(), saved.getJobGroup())) {
            jobSyncService.remove(existing.getJobName(), existing.getJobGroup());
        }
        jobSyncService.sync(saved);
        return saved;
    }

    public void delete(Long id) {
        SysJob existing = jobDao.findById(id).orElseThrow(() -> new NotFoundException("任务不存在"));
        jobDao.deleteById(id);
        jobSyncService.remove(existing.getJobName(), existing.getJobGroup());
    }

    public SysJob pause(Long id) {
        SysJob job = findOrThrow(id);
        job.setStatus(0);
        SysJob saved = jobDao.save(job);
        jobSyncService.pause(saved);
        return saved;
    }

    public SysJob resume(Long id) {
        SysJob job = findOrThrow(id);
        job.setStatus(1);
        SysJob saved = jobDao.save(job);
        jobSyncService.resume(saved);
        return saved;
    }

    public SysJob run(Long id) {
        SysJob job = findOrThrow(id);
        jobSyncService.triggerNow(job);
        return job;
    }

    private SysJob findOrThrow(Long id) {
        return jobDao.findById(id).orElseThrow(() -> new NotFoundException("任务不存在"));
    }

    private void validate(SysJob job) {
        if (job.getJobName() == null) {
            throw new IllegalArgumentException("任务名称不能为空");
        }
        if (job.getBeanName() == null) {
            throw new IllegalArgumentException("Bean 名称不能为空");
        }
        if (job.getCron() == null) {
            throw new IllegalArgumentException("cron 表达式不能为空");
        }
        if (job.getStatus() == null || (job.getStatus() != 0 && job.getStatus() != 1)) {
            throw new IllegalArgumentException("任务状态只能为 0（暂停）或 1（启用）");
        }
        validateCron(job.getCron());
        validateBean(job.getBeanName());
    }

    private void validateCron(String cron) {
        if (CronExpression.isValidExpression(cron)) {
            return;
        }
        String reason;
        try {
            new CronExpression(cron);
            reason = "无法解析";
        } catch (ParseException ex) {
            reason = ex.getMessage();
        }
        throw new IllegalArgumentException("cron 表达式不合法: " + reason);
    }

    private void validateBean(String beanName) {
        try {
            applicationContext.getBean(beanName, ScheduledTask.class);
        } catch (BeansException ex) {
            throw new IllegalArgumentException("Bean 不存在或未实现 ScheduledTask: " + beanName);
        }
    }

    private void ensureNameUnique(String jobName, String jobGroup, Long excludeId) {
        if (jobDao.countByNameAndGroup(jobName, jobGroup, excludeId) > 0) {
            throw new IllegalArgumentException("任务名称已存在：" + jobName + " (" + jobGroup + ")");
        }
    }
}
