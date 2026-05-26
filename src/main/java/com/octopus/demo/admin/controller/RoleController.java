package com.octopus.demo.admin.controller;

import com.octopus.demo.admin.entity.SysRole;
import com.octopus.demo.admin.service.RoleService;
import com.octopus.demo.admin.vo.RoleVO;
import com.octopus.demo.common.bean.R;
import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/roles")
public class RoleController {
    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping
    public R<RoleVO> create(@RequestBody SysRole role) {
        return R.ok(RoleVO.from(roleService.create(role)));
    }

    @PutMapping
    public R<RoleVO> update(@RequestBody SysRole role) {
        return R.ok(RoleVO.from(roleService.update(role)));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return R.ok();
    }

    @GetMapping("/{id}")
    public R<RoleVO> findById(@PathVariable Long id) {
        SysRole role = roleService.findById(id);
        if (role == null) return R.fail(404, "role not found");
        return R.ok(RoleVO.from(role));
    }

    @GetMapping
    public R<PageResultBean<RoleVO>> findAll(PageQueryBean query) {
        PageResultBean<SysRole> result = roleService.findAll(query);
        List<RoleVO> voList = result.getList().stream()
                .map(RoleVO::from).collect(Collectors.toList());
        PageResultBean<RoleVO> voResult = new PageResultBean<>();
        voResult.setCount(result.getCount());
        voResult.setList(voList);
        return R.ok(voResult);
    }

    @PostMapping("/{id}/resources")
    public R<RoleVO> assignResources(@PathVariable Long id, @RequestBody List<Long> resourceIds) {
        SysRole role = roleService.assignResources(id, resourceIds);
        if (role == null) return R.fail(404, "role not found");
        return R.ok(RoleVO.from(role));
    }
}
