package com.octopus.demo.admin.vo;

import com.octopus.demo.admin.entity.SysConfig;

/** Mirrors a sys_config row for the admin list page. */
public record ConfigVO(Long id, String configKey, String configValue, String remark) {

    public static ConfigVO from(SysConfig config) {
        return new ConfigVO(
                config.getId(),
                config.getConfigKey(),
                config.getConfigValue(),
                config.getRemark());
    }
}
