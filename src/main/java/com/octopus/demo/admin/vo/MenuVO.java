package com.octopus.demo.admin.vo;

import com.octopus.demo.admin.entity.SysResource;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu tree node contract shared with the web admin. The sidebar consumes
 * id/name/path/icon/children; the menu admin page additionally reads
 * parentId/type/perms/sort/status.
 */
public class MenuVO {
    private Long id;
    private Long parentId;
    private String name;
    private String type;
    private String path;
    private String icon;
    private String perms;
    private Integer sort;
    private Integer status;
    private List<MenuVO> children = new ArrayList<>();

    public MenuVO() {}

    public static MenuVO from(SysResource resource) {
        MenuVO vo = new MenuVO();
        vo.id = resource.getId();
        vo.parentId = resource.getParentId();
        vo.name = resource.getName();
        vo.type = resource.getType();
        vo.path = resource.getPath();
        vo.icon = resource.getIcon();
        vo.perms = resource.getCode();
        vo.sort = resource.getSort();
        vo.status = resource.getStatus();
        return vo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public String getPerms() { return perms; }
    public void setPerms(String perms) { this.perms = perms; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public List<MenuVO> getChildren() { return children; }
    public void setChildren(List<MenuVO> children) { this.children = children; }
}
