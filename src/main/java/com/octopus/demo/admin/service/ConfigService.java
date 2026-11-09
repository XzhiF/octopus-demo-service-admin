package com.octopus.demo.admin.service;

import com.octopus.demo.admin.dao.ConfigDao;
import com.octopus.demo.admin.dto.ConfigRequest;
import com.octopus.demo.admin.entity.SysConfig;
import com.octopus.demo.admin.exception.NotFoundException;
import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class ConfigService {

    private static final Pattern KEY_PATTERN = Pattern.compile("[A-Za-z0-9._-]+");

    private final ConfigDao configDao;

    public ConfigService(ConfigDao configDao) {
        this.configDao = configDao;
    }

    public PageResultBean<SysConfig> findAll(PageQueryBean query, String key) {
        return configDao.findAll(query, key == null ? null : key.trim());
    }

    public SysConfig create(ConfigRequest request) {
        SysConfig entity = request.toEntity(null);
        validate(entity);
        ensureKeyUnique(entity.getConfigKey(), null);
        return configDao.save(entity);
    }

    public SysConfig update(Long id, ConfigRequest request) {
        configDao.findById(id).orElseThrow(() -> new NotFoundException("配置不存在"));
        SysConfig entity = request.toEntity(id);
        validate(entity);
        ensureKeyUnique(entity.getConfigKey(), id);
        return configDao.save(entity);
    }

    public void delete(Long id) {
        configDao.findById(id).orElseThrow(() -> new NotFoundException("配置不存在"));
        configDao.deleteById(id);
    }

    public String getValue(String configKey) {
        return configDao.findByKey(configKey)
                .map(SysConfig::getConfigValue)
                .orElseThrow(() -> new NotFoundException("配置不存在：" + configKey));
    }

    private void validate(SysConfig entity) {
        String key = entity.getConfigKey();
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("参数键不能为空");
        }
        if (!KEY_PATTERN.matcher(key).matches()) {
            throw new IllegalArgumentException("参数键只允许字母、数字、点、下划线和短横线");
        }
        if (entity.getConfigValue() == null) {
            throw new IllegalArgumentException("参数值不能为空");
        }
    }

    private void ensureKeyUnique(String configKey, Long excludeId) {
        if (configDao.countByKey(configKey, excludeId) > 0) {
            throw new IllegalArgumentException("参数键已存在：" + configKey);
        }
    }
}
