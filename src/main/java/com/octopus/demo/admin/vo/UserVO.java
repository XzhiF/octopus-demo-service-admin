package com.octopus.demo.admin.vo;

import com.octopus.demo.admin.entity.SysUser;
import java.util.List;
import java.util.stream.Collectors;

public class UserVO {
    private Long id;
    private String username;
    private List<RoleVO> roles;

    public UserVO() {}

    public static UserVO from(SysUser user) {
        UserVO vo = new UserVO();
        vo.id = user.getId();
        vo.username = user.getUsername();
        vo.roles = user.getRoles().stream()
                .map(RoleVO::from).collect(Collectors.toList());
        return vo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public List<RoleVO> getRoles() { return roles; }
    public void setRoles(List<RoleVO> roles) { this.roles = roles; }
}
