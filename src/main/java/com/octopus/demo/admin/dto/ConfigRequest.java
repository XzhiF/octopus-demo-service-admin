package com.octopus.demo.admin.dto;

import com.octopus.demo.admin.entity.SysConfig;

/** Body of POST /api/configs and PUT /api/configs/{id}. */
public class ConfigRequest {
    private String configKey;
    private String configValue;
    private String remark;

    public SysConfig toEntity(Long id) {
        SysConfig entity = new SysConfig();
        entity.setId(id);
        entity.setConfigKey(configKey == null ? null : configKey.trim());
        entity.setConfigValue(configValue);
        entity.setRemark(remark == null || remark.isBlank() ? null : remark.trim());
        return entity;
    }

    public String getConfigKey() { return configKey; }
    public void setConfigKey(String configKey) { this.configKey = configKey; }
    public String getConfigValue() { return configValue; }
    public void setConfigValue(String configValue) { this.configValue = configValue; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
