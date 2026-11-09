package com.octopus.demo.admin.vo;

import com.octopus.demo.admin.security.AuthContext;

import java.util.List;

public class AuthVO {
    private String token;
    private String tokenType;
    private long expiresInSeconds;
    private UserVO user;
    private List<String> permissions;

    public AuthVO() {}

    public static AuthVO of(String token, long expiresInSeconds, AuthContext context) {
        AuthVO vo = new AuthVO();
        vo.token = token;
        vo.tokenType = "Bearer";
        vo.expiresInSeconds = expiresInSeconds;
        vo.user = UserVO.of(context.userId(), context.username(), context.nickname(), context.roleCodes());
        vo.permissions = context.permissions();
        return vo;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }
    public long getExpiresInSeconds() { return expiresInSeconds; }
    public void setExpiresInSeconds(long expiresInSeconds) { this.expiresInSeconds = expiresInSeconds; }
    public UserVO getUser() { return user; }
    public void setUser(UserVO user) { this.user = user; }
    public List<String> getPermissions() { return permissions; }
    public void setPermissions(List<String> permissions) { this.permissions = permissions; }
}
