package com.octopus.demo.admin.entity;

import java.util.ArrayList;
import java.util.List;

public class SysRole {
    private Long id;
    private String name;
    private String code;
    private List<SysResource> resources = new ArrayList<>();

    public SysRole() {}

    public SysRole(Long id, String name, String code) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.resources = new ArrayList<>();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public List<SysResource> getResources() { return resources; }
    public void setResources(List<SysResource> resources) { this.resources = resources; }
}