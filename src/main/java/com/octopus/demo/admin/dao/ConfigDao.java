package com.octopus.demo.admin.dao;

import com.octopus.demo.admin.entity.SysConfig;
import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;

import java.util.Optional;

public interface ConfigDao {
    SysConfig save(SysConfig config);

    Optional<SysConfig> findById(Long id);

    Optional<SysConfig> findByKey(String configKey);

    PageResultBean<SysConfig> findAll(PageQueryBean query, String key);

    void deleteById(Long id);

    int countByKey(String configKey, Long excludeId);
}
