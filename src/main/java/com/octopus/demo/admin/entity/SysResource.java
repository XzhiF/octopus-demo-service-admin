package com.octopus.demo.admin.entity;

/**
 * A row of {@code sys_menu}: catalogs, menus and button permissions.
 * {@code code} carries {@code sys_menu.perms}.
 */
public class SysResource {
    private Long id;
    private Long parentId;
    private String name;
    private String code;
    private String type;
    private String path;
    private String component;
    private String icon;
    private Integer sort;
    private Integer visible;
    private Integer status;

    public SysResource() {}

    public SysResource(Long id, String name, String code, String type) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.type = type;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getComponent() { return component; }
    public void setComponent(String component) { this.component = component; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }

    public Integer getVisible() { return visible; }
    public void setVisible(Integer visible) { this.visible = visible; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
