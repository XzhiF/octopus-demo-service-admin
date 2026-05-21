package com.octopus.demo.admin.controller;

import com.octopus.demo.admin.entity.Result;
import com.octopus.demo.admin.entity.SysUser;
import com.octopus.demo.admin.service.UserService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public Result<SysUser> create(@RequestBody SysUser user) {
        return Result.ok(userService.create(user));
    }

    @PutMapping
    public Result<SysUser> update(@RequestBody SysUser user) {
        return Result.ok(userService.update(user));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }

    @GetMapping("/{id}")
    public Result<SysUser> findById(@PathVariable Long id) {
        SysUser user = userService.findById(id);
        if (user == null) return Result.error(404, "user not found");
        user.setPassword(null);
        return Result.ok(user);
    }

    @GetMapping
    public Result<List<SysUser>> findAll() {
        List<SysUser> users = userService.findAll();
        users.forEach(u -> u.setPassword(null));
        return Result.ok(users);
    }

    @PostMapping("/{id}/roles")
    public Result<SysUser> assignRoles(@PathVariable Long id, @RequestBody List<Long> roleIds) {
        SysUser user = userService.assignRoles(id, roleIds);
        if (user == null) return Result.error(404, "user not found");
        user.setPassword(null);
        return Result.ok(user);
    }
}