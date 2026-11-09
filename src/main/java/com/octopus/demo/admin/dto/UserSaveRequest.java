package com.octopus.demo.admin.dto;

import java.util.List;

/** Payload for POST /api/users and PUT /api/users/{id}. */
public record UserSaveRequest(
        String username,
        String nickname,
        String gender,
        String password,
        Integer status,
        List<Long> roleIds
) {
}
