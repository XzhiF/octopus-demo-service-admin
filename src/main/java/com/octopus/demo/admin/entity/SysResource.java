package com.octopus.demo.admin.entity;

public class SysResource {
    private Long id;
    private String name;
    private String code;
    private String type;

    public SysResource() {}

    public SysResource(Long id, String name, String code, String type) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.type = type;
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