package com.octopus.demo.admin.entity;

public class SysDictType {
    private Long id;
    private String dictType;
    private String dictName;
    private Integer status = 1;
    private String remark;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDictType() { return dictType; }
    public void setDictType(String dictType) { this.dictType = dictType; }

    public String getDictName() { return dictName; }
    public void setDictName(String dictName) { this.dictName = dictName; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
