package com.octopus.demo.admin.entity;

/** A row of {@code sys_config}: a key/value runtime parameter. */
public class SysConfig {
    private Long id;
    private String configKey;
    private String configValue;
    private String remark;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getConfigKey() { return configKey; }
    public void setConfigKey(String configKey) { this.configKey = configKey; }

    public String getConfigValue() { return configValue; }
    public void setConfigValue(String configValue) { this.configValue = configValue; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
