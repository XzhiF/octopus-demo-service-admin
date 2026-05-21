package com.octopus.demo.admin.controller;

import com.octopus.demo.admin.entity.Result;
import com.octopus.demo.admin.entity.SysResource;
import com.octopus.demo.admin.service.ResourceService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {
    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @PostMapping
    public Result<SysResource> create(@RequestBody SysResource resource) {
        return Result.ok(resourceService.create(resource));
    }

    @PutMapping
    public Result<SysResource> update(@RequestBody SysResource resource) {
        return Result.ok(resourceService.update(resource));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        resourceService.delete(id);
        return Result.ok();
    }

    @GetMapping("/{id}")
    public Result<SysResource> findById(@PathVariable Long id) {
        SysResource resource = resourceService.findById(id);
        if (resource == null) return Result.error(404, "resource not found");
        return Result.ok(resource);
    }

    @GetMapping
    public Result<List<SysResource>> findAll() {
        return Result.ok(resourceService.findAll());
    }
}