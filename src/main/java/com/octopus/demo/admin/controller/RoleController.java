package com.octopus.demo.admin.controller;

import com.octopus.demo.admin.entity.Result;
import com.octopus.demo.admin.entity.SysRole;
import com.octopus.demo.admin.service.RoleService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RoleController {
    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping
    public Result<SysRole> create(@RequestBody SysRole role) {
        return Result.ok(roleService.create(role));
    }

    @PutMapping
    public Result<SysRole> update(@RequestBody SysRole role) {
        return Result.ok(roleService.update(role));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.ok();
    }

    @GetMapping("/{id}")
    public Result<SysRole> findById(@PathVariable Long id) {
        SysRole role = roleService.findById(id);
        if (role == null) return Result.error(404, "role not found");
        return Result.ok(role);
    }

    @GetMapping
    public Result<List<SysRole>> findAll() {
        return Result.ok(roleService.findAll());
    }

    @PostMapping("/{id}/resources")
    public Result<SysRole> assignResources(@PathVariable Long id, @RequestBody List<Long> resourceIds) {
        SysRole role = roleService.assignResources(id, resourceIds);
        if (role == null) return Result.error(404, "role not found");
        return Result.ok(role);
    }
}