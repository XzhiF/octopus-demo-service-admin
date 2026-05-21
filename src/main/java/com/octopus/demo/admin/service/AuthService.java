package com.octopus.demo.admin.service;

import com.octopus.demo.admin.dao.UserDao;
import com.octopus.demo.admin.entity.SysUser;
import com.octopus.demo.admin.entity.SysResource;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class AuthService {
    private final UserDao userDao;
    private final Map<String, SysUser> sessions = new ConcurrentHashMap<>();

    public AuthService(UserDao userDao) {
        this.userDao = userDao;
    }

    public String login(String username, String password) {
        SysUser user = userDao.findByUsername(username).orElse(null);
        if (user == null || !user.getPassword().equals(password)) {
            return null;
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        sessions.put(token, user);
        return token;
    }

    public void logout(String token) {
        sessions.remove(token);
    }

    public SysUser getCurrentUser(String token) {
        return sessions.get(token);
    }

    public List<SysResource> getUserResources(String token) {
        SysUser user = sessions.get(token);
        if (user == null) return Collections.emptyList();
        return user.getRoles().stream()
            .flatMap(role -> role.getResources().stream())
            .distinct()
            .collect(Collectors.toList());
    }

    public boolean hasPermission(String token, String resourceCode) {
        return getUserResources(token).stream()
            .anyMatch(r -> r.getCode().equals(resourceCode));
    }
}