package com.octopus.demo.admin.dto;

import com.octopus.demo.admin.entity.SysResource;

/** Body of POST /api/menus and PUT /api/menus/{id}; {@code perms} is sys_menu.perms. */
public class MenuRequest {
    private Long parentId;
    private String name;
    private String type;
    private String path;
    private String component;
    private String icon;
    private String perms;
    private Integer sort;
    private Integer visible;
    private Integer status;

    public SysResource toEntity(Long id) {
        SysResource entity = new SysResource();
        entity.setId(id);
        entity.setParentId(parentId == null ? Long.valueOf(0L) : parentId);
        entity.setName(name == null ? null : name.trim());
        entity.setType(type == null ? null : type.trim());
        entity.setPath(blankToNull(path));
        entity.setComponent(blankToNull(component));
        entity.setIcon(blankToNull(icon));
        entity.setCode(blankToNull(perms));
        entity.setSort(sort);
        entity.setVisible(visible);
        entity.setStatus(status);
        return entity;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getComponent() { return component; }
    public void setComponent(String component) { this.component = component; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public String getPerms() { return perms; }
    public void setPerms(String perms) { this.perms = perms; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public Integer getVisible() { return visible; }
    public void setVisible(Integer visible) { this.visible = visible; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
