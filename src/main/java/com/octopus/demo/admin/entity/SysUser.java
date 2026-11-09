package com.octopus.demo.admin.entity;

import java.util.ArrayList;
import java.util.List;

public class SysUser {
    private Long id;
    private String username;
    private String password;
    private String nickname;
    /** Dictionary value from {@code sys_user_sex}; null when not set. */
    private String gender;
    private Integer status = 1;
    private List<SysRole> roles = new ArrayList<>();

    public SysUser() {}

    public SysUser(Long id, String username, String password) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.roles = new ArrayList<>();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public List<SysRole> getRoles() { return roles; }
    public void setRoles(List<SysRole> roles) { this.roles = roles; }
}
