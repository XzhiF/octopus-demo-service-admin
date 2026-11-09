package com.octopus.demo.admin.vo;

import com.octopus.demo.admin.entity.SysRole;
import java.util.List;
import java.util.Collections;

public class RoleVO {
    private Long id;
    private String name;
    private String code;
    private Integer status;
    private boolean builtIn;
    private List<ResourceVO> resources;

    public RoleVO() {}

    public static RoleVO from(SysRole role) {
        RoleVO vo = new RoleVO();
        vo.id = role.getId();
        vo.name = role.getName();
        vo.code = role.getCode();
        vo.status = role.getStatus() == null ? 1 : role.getStatus();
        vo.builtIn = "admin".equals(role.getCode());
        vo.resources = role.getResources() == null ? Collections.emptyList()
                : role.getResources().stream().map(ResourceVO::from).toList();
        return vo;
    }

    public static RoleVO codeOnly(String code) {
        RoleVO vo = new RoleVO();
        vo.code = code;
        vo.resources = Collections.emptyList();
        return vo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public boolean isBuiltIn() { return builtIn; }
    public void setBuiltIn(boolean builtIn) { this.builtIn = builtIn; }
    public List<ResourceVO> getResources() { return resources; }
    public void setResources(List<ResourceVO> resources) { this.resources = resources; }
}
