package com.octopus.demo.admin.vo;

import com.octopus.demo.admin.entity.SysDictData;

/** Option shape consumed by frontend dropdowns: label/value. */
public class DictDataVO {
    private final String label;
    private final String value;

    private DictDataVO(SysDictData data) {
        this.label = data.getDictLabel();
        this.value = data.getDictValue();
    }

    public static DictDataVO from(SysDictData data) {
        return new DictDataVO(data);
    }

    public String getLabel() { return label; }
    public String getValue() { return value; }
}
