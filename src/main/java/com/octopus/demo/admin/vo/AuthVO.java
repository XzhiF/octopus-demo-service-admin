package com.octopus.demo.admin.vo;

public class AuthVO {
    private String token;
    private UserVO user;

    public AuthVO() {}

    public static AuthVO of(String token, UserVO user) {
        AuthVO vo = new AuthVO();
        vo.token = token;
        vo.user = user;
        return vo;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public UserVO getUser() { return user; }
    public void setUser(UserVO user) { this.user = user; }
}
