package com.octopus.demo.admin.vo;

import com.octopus.demo.admin.entity.SysUser;
import java.util.List;
import java.util.Collections;

public class UserVO {
    private Long id;
    private String username;
    private String nickname;
    private Integer status;
    private boolean builtIn;
    private List<RoleVO> roles;

    public UserVO() {}

    public static UserVO from(SysUser user) {
        UserVO vo = new UserVO();
        vo.id = user.getId();
        vo.username = user.getUsername();
        vo.nickname = user.getNickname();
        vo.status = user.getStatus();
        vo.builtIn = isBuiltIn(user.getId(), user.getUsername());
        vo.roles = user.getRoles() == null ? Collections.emptyList()
                : user.getRoles().stream().map(RoleVO::from).toList();
        return vo;
    }

    public static UserVO of(Long id, String username, String nickname, List<String> roleCodes) {
        UserVO vo = new UserVO();
        vo.id = id;
        vo.username = username;
        vo.nickname = nickname;
        vo.status = 1;
        vo.builtIn = isBuiltIn(id, username);
        vo.roles = roleCodes == null ? Collections.emptyList()
                : roleCodes.stream().map(RoleVO::codeOnly).toList();
        return vo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public boolean isBuiltIn() { return builtIn; }
    public void setBuiltIn(boolean builtIn) { this.builtIn = builtIn; }
    public List<RoleVO> getRoles() { return roles; }
    public void setRoles(List<RoleVO> roles) { this.roles = roles; }

    private static boolean isBuiltIn(Long id, String username) {
        return (id != null && id == 1L) || "admin".equals(username);
    }
}
