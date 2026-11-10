package com.octopus.demo.admin.service;

import com.octopus.demo.admin.dao.JobLogDao;
import com.octopus.demo.admin.entity.SysJobLog;
import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class JobLogService {

    private final JobLogDao jobLogDao;

    public JobLogService(JobLogDao jobLogDao) {
        this.jobLogDao = jobLogDao;
    }

    public PageResultBean<SysJobLog> findAll(PageQueryBean query, Long jobId, Integer status,
                                             LocalDateTime from, LocalDateTime to) {
        return jobLogDao.findAll(query, jobId, status, from, to);
    }
}
