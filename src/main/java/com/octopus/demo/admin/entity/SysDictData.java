package com.octopus.demo.admin.entity;

public class SysDictData {
    private Long id;
    private String dictType;
    private String dictLabel;
    private String dictValue;
    private Integer sort = 0;
    private Integer status = 1;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDictType() { return dictType; }
    public void setDictType(String dictType) { this.dictType = dictType; }

    public String getDictLabel() { return dictLabel; }
    public void setDictLabel(String dictLabel) { this.dictLabel = dictLabel; }

    public String getDictValue() { return dictValue; }
    public void setDictValue(String dictValue) { this.dictValue = dictValue; }

    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
