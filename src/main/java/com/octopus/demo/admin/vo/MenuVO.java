package com.octopus.demo.admin.vo;

import com.octopus.demo.admin.entity.SysResource;

import java.util.ArrayList;
import java.util.List;

/** Menu tree node contract shared with the web admin: id, name, path, icon, children. */
public class MenuVO {
    private Long id;
    private String name;
    private String path;
    private String icon;
    private List<MenuVO> children = new ArrayList<>();

    public MenuVO() {}

    public static MenuVO from(SysResource resource) {
        MenuVO vo = new MenuVO();
        vo.id = resource.getId();
        vo.name = resource.getName();
        vo.path = resource.getPath();
        vo.icon = resource.getIcon();
        return vo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public List<MenuVO> getChildren() { return children; }
    public void setChildren(List<MenuVO> children) { this.children = children; }
}
