package com.octopus.demo.admin.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Authenticated principal built from {@code sys_user} + roles + menu permission codes.
 * Roles become {@code ROLE_<code>}; menu perms become {@code perm:<perms>}.
 */
public record AuthContext(
        Long userId,
        String username,
        String nickname,
        List<String> roleCodes,
        List<String> permissions) {

    public Collection<SimpleGrantedAuthority> authorities() {
        Stream<String> roles = roleCodes.stream()
                .filter(Objects::nonNull)
                .map(code -> "ROLE_" + code);
        Stream<String> perms = permissions.stream()
                .filter(Objects::nonNull)
                .map(perm -> "perm:" + perm);
        return Stream.concat(roles, perms).map(SimpleGrantedAuthority::new).toList();
    }
}
