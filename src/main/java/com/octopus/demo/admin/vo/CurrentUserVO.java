package com.octopus.demo.admin.vo;

import com.octopus.demo.admin.security.AuthContext;

import java.util.List;

public record CurrentUserVO(UserVO user, List<String> roles, List<String> permissions) {

    public static CurrentUserVO from(AuthContext context) {
        return new CurrentUserVO(
                UserVO.of(context.userId(), context.username(), context.nickname(), context.roleCodes()),
                context.roleCodes(),
                context.permissions());
    }
}
