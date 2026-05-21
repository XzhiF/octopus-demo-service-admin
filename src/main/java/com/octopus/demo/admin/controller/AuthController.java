package com.octopus.demo.admin.controller;

import com.octopus.demo.admin.entity.Result;
import com.octopus.demo.admin.entity.SysUser;
import com.octopus.demo.admin.entity.SysResource;
import com.octopus.demo.admin.service.AuthService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String token = authService.login(username, password);
        if (token == null) {
            return Result.error(401, "invalid username or password");
        }
        SysUser user = authService.getCurrentUser(token);
        user.setPassword(null);
        return Result.ok(Map.of("token", token, "user", user));
    }

    @PostMapping("/logout")
    public Result<?> logout(@RequestHeader("X-Token") String token) {
        authService.logout(token);
        return Result.ok();
    }

    @GetMapping("/current")
    public Result<SysUser> current(@RequestHeader("X-Token") String token) {
        SysUser user = authService.getCurrentUser(token);
        if (user == null) return Result.error(401, "not logged in");
        user.setPassword(null);
        return Result.ok(user);
    }

    @GetMapping("/resources")
    public Result<List<SysResource>> resources(@RequestHeader("X-Token") String token) {
        List<SysResource> list = authService.getUserResources(token);
        if (list.isEmpty()) return Result.error(401, "not logged in");
        return Result.ok(list);
    }
}