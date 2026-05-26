package com.octopus.demo.admin.vo;

import com.octopus.demo.admin.entity.SysRole;
import java.util.List;
import java.util.stream.Collectors;

public class RoleVO {
    private Long id;
    private String name;
    private String code;
    private List<ResourceVO> resources;

    public RoleVO() {}

    public static RoleVO from(SysRole role) {
        RoleVO vo = new RoleVO();
        vo.id = role.getId();
        vo.name = role.getName();
        vo.code = role.getCode();
        vo.resources = role.getResources().stream()
                .map(ResourceVO::from).collect(Collectors.toList());
        return vo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public List<ResourceVO> getResources() { return resources; }
    public void setResources(List<ResourceVO> resources) { this.resources = resources; }
}
