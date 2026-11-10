package com.octopus.demo.admin.dao.impl;

import com.octopus.demo.admin.dao.JobDao;
import com.octopus.demo.admin.entity.SysJob;
import com.octopus.demo.admin.mapper.JobMapper;
import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MybatisJobDao implements JobDao {

    private final JobMapper jobMapper;

    public MybatisJobDao(JobMapper jobMapper) {
        this.jobMapper = jobMapper;
    }

    @Override
    public SysJob save(SysJob job) {
        if (job.getId() == null) {
            jobMapper.insert(job);
        } else {
            jobMapper.update(job);
        }
        return job;
    }

    @Override
    public Optional<SysJob> findById(Long id) {
        return Optional.ofNullable(jobMapper.findById(id));
    }

    @Override
    public PageResultBean<SysJob> findAll(PageQueryBean query, String jobName, Integer status) {
        long total = jobMapper.count(jobName, status);
        long offset = (long) (query.getPage() - 1) * query.getSize();
        PageResultBean<SysJob> result = new PageResultBean<>();
        result.setCount(total);
        if (offset >= total) {
            result.setList(List.of());
            return result;
        }
        result.setList(jobMapper.findPage(jobName, status, offset, query.getSize()));
        return result;
    }

    @Override
    public List<SysJob> findAllEnabled() {
        return jobMapper.findAllEnabled();
    }

    @Override
    public void deleteById(Long id) {
        jobMapper.deleteById(id);
    }

    @Override
    public int countByNameAndGroup(String jobName, String jobGroup, Long excludeId) {
        return jobMapper.countByNameAndGroup(jobName, jobGroup, excludeId);
    }
}
