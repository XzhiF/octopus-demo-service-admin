package com.octopus.demo.admin.dao.impl;

import com.octopus.demo.admin.dao.OperLogDao;
import com.octopus.demo.admin.entity.SysOperLog;
import com.octopus.demo.admin.mapper.OperLogMapper;
import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class MybatisOperLogDao implements OperLogDao {

    private final OperLogMapper operLogMapper;

    public MybatisOperLogDao(OperLogMapper operLogMapper) {
        this.operLogMapper = operLogMapper;
    }

    @Override
    public SysOperLog save(SysOperLog log) {
        operLogMapper.insert(log);
        return log;
    }

    @Override
    public SysOperLog findById(Long id) {
        return operLogMapper.findById(id);
    }

    @Override
    public PageResultBean<SysOperLog> findAll(PageQueryBean query, String operator, String module,
                                              Integer status, LocalDateTime from, LocalDateTime to) {
        long total = operLogMapper.count(operator, module, status, from, to);
        long offset = (long) (query.getPage() - 1) * query.getSize();
        PageResultBean<SysOperLog> result = new PageResultBean<>();
        result.setCount(total);
        if (offset >= total) {
            result.setList(List.of());
            return result;
        }
        result.setList(operLogMapper.findPage(operator, module, status, from, to, offset, query.getSize()));
        return result;
    }
}
