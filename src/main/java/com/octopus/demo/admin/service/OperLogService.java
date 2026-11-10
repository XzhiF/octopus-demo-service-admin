package com.octopus.demo.admin.service;

import com.octopus.demo.admin.dao.OperLogDao;
import com.octopus.demo.admin.entity.SysOperLog;
import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class OperLogService {

    private final OperLogDao operLogDao;

    public OperLogService(OperLogDao operLogDao) {
        this.operLogDao = operLogDao;
    }

    public SysOperLog save(SysOperLog log) {
        log.setTitle(truncate(log.getTitle(), 128));
        log.setModule(truncate(log.getModule(), 64));
        log.setHttpMethod(truncate(log.getHttpMethod(), 16));
        log.setRequestUri(truncate(log.getRequestUri(), 255));
        log.setOperator(truncate(log.getOperator(), 64));
        log.setIp(truncate(log.getIp(), 128));
        log.setErrorMsg(truncate(log.getErrorMsg(), 500));
        return operLogDao.save(log);
    }

    public SysOperLog findById(Long id) {
        return operLogDao.findById(id);
    }

    public PageResultBean<SysOperLog> findAll(PageQueryBean query, String operator, String module,
                                              Integer status, LocalDateTime from, LocalDateTime to) {
        return operLogDao.findAll(query, operator, module, status, from, to);
    }

    /** Keeps client-controlled values (IP header, URI) within the column widths. */
    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
