package com.octopus.demo.admin.dao.impl;

import com.octopus.demo.admin.dao.ConfigDao;
import com.octopus.demo.admin.entity.SysConfig;
import com.octopus.demo.admin.mapper.ConfigMapper;
import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MybatisConfigDao implements ConfigDao {

    private final ConfigMapper configMapper;

    public MybatisConfigDao(ConfigMapper configMapper) {
        this.configMapper = configMapper;
    }

    @Override
    public SysConfig save(SysConfig config) {
        if (config.getId() == null) {
            configMapper.insert(config);
        } else {
            configMapper.update(config);
        }
        return config;
    }

    @Override
    public Optional<SysConfig> findById(Long id) {
        return Optional.ofNullable(configMapper.findById(id));
    }

    @Override
    public Optional<SysConfig> findByKey(String configKey) {
        return Optional.ofNullable(configMapper.findByKey(configKey));
    }

    @Override
    public PageResultBean<SysConfig> findAll(PageQueryBean query, String key) {
        long total = configMapper.count(key);
        long offset = (long) (query.getPage() - 1) * query.getSize();
        PageResultBean<SysConfig> result = new PageResultBean<>();
        result.setCount(total);
        if (offset >= total) {
            result.setList(List.of());
            return result;
        }
        result.setList(configMapper.findPage(key, offset, query.getSize()));
        return result;
    }

    @Override
    public void deleteById(Long id) {
        configMapper.deleteById(id);
    }

    @Override
    public int countByKey(String configKey, Long excludeId) {
        return configMapper.countByKey(configKey, excludeId);
    }
}
