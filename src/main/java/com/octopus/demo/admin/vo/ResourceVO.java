package com.octopus.demo.admin.vo;

import com.octopus.demo.admin.entity.SysResource;

public class ResourceVO {
    private Long id;
    private String name;
    private String code;
    private String type;

    public ResourceVO() {}

    public static ResourceVO from(SysResource resource) {
        ResourceVO vo = new ResourceVO();
        vo.id = resource.getId();
        vo.name = resource.getName();
        vo.code = resource.getCode();
        vo.type = resource.getType();
        return vo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
