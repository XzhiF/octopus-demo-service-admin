package com.octopus.demo.admin.vo;

import com.octopus.demo.admin.entity.SysDictType;

public class DictTypeVO {
    private final Long id;
    private final String dictType;
    private final String dictName;
    private final Integer status;
    private final String remark;

    private DictTypeVO(SysDictType type) {
        this.id = type.getId();
        this.dictType = type.getDictType();
        this.dictName = type.getDictName();
        this.status = type.getStatus();
        this.remark = type.getRemark();
    }

    public static DictTypeVO from(SysDictType type) {
        return new DictTypeVO(type);
    }

    public Long getId() { return id; }
    public String getDictType() { return dictType; }
    public String getDictName() { return dictName; }
    public Integer getStatus() { return status; }
    public String getRemark() { return remark; }
}
