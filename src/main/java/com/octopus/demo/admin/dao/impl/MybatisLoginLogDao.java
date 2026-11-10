package com.octopus.demo.admin.dao.impl;

import com.octopus.demo.admin.dao.LoginLogDao;
import com.octopus.demo.admin.entity.SysLoginLog;
import com.octopus.demo.admin.mapper.LoginLogMapper;
import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class MybatisLoginLogDao implements LoginLogDao {

    private final LoginLogMapper loginLogMapper;

    public MybatisLoginLogDao(LoginLogMapper loginLogMapper) {
        this.loginLogMapper = loginLogMapper;
    }

    @Override
    public SysLoginLog save(SysLoginLog log) {
        loginLogMapper.insert(log);
        return log;
    }

    @Override
    public PageResultBean<SysLoginLog> findAll(PageQueryBean query, String username, Integer status,
                                               LocalDateTime from, LocalDateTime to) {
        long total = loginLogMapper.count(username, status, from, to);
        long offset = (long) (query.getPage() - 1) * query.getSize();
        PageResultBean<SysLoginLog> result = new PageResultBean<>();
        result.setCount(total);
        if (offset >= total) {
            result.setList(List.of());
            return result;
        }
        result.setList(loginLogMapper.findPage(username, status, from, to, offset, query.getSize()));
        return result;
    }
}
