package com.octopus.demo.admin.dao.impl;

import com.octopus.demo.admin.dao.JobLogDao;
import com.octopus.demo.admin.entity.SysJobLog;
import com.octopus.demo.admin.mapper.JobLogMapper;
import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class MybatisJobLogDao implements JobLogDao {

    private final JobLogMapper jobLogMapper;

    public MybatisJobLogDao(JobLogMapper jobLogMapper) {
        this.jobLogMapper = jobLogMapper;
    }

    @Override
    public SysJobLog save(SysJobLog log) {
        jobLogMapper.insert(log);
        return log;
    }

    @Override
    public PageResultBean<SysJobLog> findAll(PageQueryBean query, Long jobId, Integer status,
                                             LocalDateTime from, LocalDateTime to) {
        long total = jobLogMapper.count(jobId, status, from, to);
        long offset = (long) (query.getPage() - 1) * query.getSize();
        PageResultBean<SysJobLog> result = new PageResultBean<>();
        result.setCount(total);
        if (offset >= total) {
            result.setList(List.of());
            return result;
        }
        result.setList(jobLogMapper.findPage(jobId, status, from, to, offset, query.getSize()));
        return result;
    }
}
