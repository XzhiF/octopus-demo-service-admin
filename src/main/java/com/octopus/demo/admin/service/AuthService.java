package com.octopus.demo.admin.service;

import com.octopus.demo.admin.dao.ResourceDao;
import com.octopus.demo.admin.dao.UserDao;
import com.octopus.demo.admin.entity.SysResource;
import com.octopus.demo.admin.entity.SysRole;
import com.octopus.demo.admin.entity.SysUser;
import com.octopus.demo.admin.exception.DisabledAccountException;
import com.octopus.demo.admin.security.AuthContext;
import com.octopus.demo.admin.security.JwtProperties;
import com.octopus.demo.admin.vo.MenuVO;
import com.octopus.demo.common.util.JwtUtil;
import com.octopus.demo.common.util.PasswordUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class AuthService {

    private final UserDao userDao;
    private final ResourceDao resourceDao;
    private final JwtProperties jwtProperties;

    public AuthService(UserDao userDao, ResourceDao resourceDao, JwtProperties jwtProperties) {
        this.userDao = userDao;
        this.resourceDao = resourceDao;
        this.jwtProperties = jwtProperties;
    }

    /** Verifies credentials and returns the authenticated principal; empty on any failure. */
    public Optional<AuthContext> authenticate(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            return Optional.empty();
        }
        SysUser user = userDao.findByUsername(username).orElse(null);
        if (user == null || !PasswordUtils.matches(password, user.getPassword())) {
            return Optional.empty();
        }
        if (!isEnabled(user)) {
            throw new DisabledAccountException("账号已停用");
        }
        return Optional.of(toContext(user));
    }

    public Optional<AuthContext> loadById(Long userId) {
        SysUser user = userDao.findById(userId).orElse(null);
        if (user == null || !isEnabled(user)) {
            return Optional.empty();
        }
        return Optional.of(toContext(user));
    }

    public String issueToken(Long userId) {
        return JwtUtil.generateToken(userId);
    }

    public long expirationSeconds() {
        return jwtProperties.expirationDays() * 24L * 60L * 60L;
    }

    /** Visible catalog/menu tree (buttons excluded) granted to the user. */
    public List<MenuVO> menuTree(Long userId) {
        List<SysResource> menus = resourceDao.findVisibleByUserId(userId);
        Map<Long, MenuVO> byId = new LinkedHashMap<>();
        for (SysResource menu : menus) {
            byId.put(menu.getId(), MenuVO.from(menu));
        }
        List<MenuVO> roots = new ArrayList<>();
        for (SysResource menu : menus) {
            MenuVO node = byId.get(menu.getId());
            Long parentId = menu.getParentId();
            MenuVO parent = parentId == null ? null : byId.get(parentId);
            if (parent == null) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        return roots;
    }

    private boolean isEnabled(SysUser user) {
        return user.getStatus() == null || user.getStatus() == 1;
    }

    private AuthContext toContext(SysUser user) {
        List<SysRole> roles = user.getRoles() == null ? List.of() : user.getRoles();
        List<String> roleCodes = roles.stream()
                .map(SysRole::getCode)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<String> permissions = roles.stream()
                .flatMap(role -> role.getResources() == null ? List.<SysResource>of().stream()
                        : role.getResources().stream())
                .map(SysResource::getCode)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
        return new AuthContext(user.getId(), user.getUsername(), user.getNickname(), roleCodes, permissions);
    }
}
