package com.octopus.demo.admin.controller;

import com.octopus.demo.admin.annotation.OperLog;
import com.octopus.demo.admin.entity.SysRole;
import com.octopus.demo.admin.service.GrantService;
import com.octopus.demo.admin.service.RoleService;
import com.octopus.demo.admin.vo.RoleVO;
import com.octopus.demo.common.bean.R;
import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/roles")
public class RoleController {
    private final RoleService roleService;
    private final GrantService grantService;

    public RoleController(RoleService roleService, GrantService grantService) {
        this.roleService = roleService;
        this.grantService = grantService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('perm:sys:role:add')")
    @OperLog(title = "新增角色", module = "role")
    public R<RoleVO> create(@RequestBody SysRole role) {
        return R.ok(RoleVO.from(roleService.create(role)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('perm:sys:role:update')")
    @OperLog(title = "修改角色", module = "role")
    public R<RoleVO> update(@PathVariable Long id, @RequestBody SysRole role) {
        return R.ok(RoleVO.from(roleService.update(id, role)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('perm:sys:role:delete')")
    @OperLog(title = "删除角色", module = "role")
    public R<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return R.ok();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('perm:sys:role:update')")
    @OperLog(title = "角色状态变更", module = "role")
    public R<RoleVO> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        return R.ok(RoleVO.from(roleService.updateStatus(id, body.get("status"))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('perm:sys:role:list')")
    public R<RoleVO> findById(@PathVariable Long id) {
        SysRole role = roleService.findById(id);
        if (role == null) return R.fail(404, "role not found");
        return R.ok(RoleVO.from(role));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('perm:sys:role:list')")
    public R<PageResultBean<RoleVO>> findAll(PageQueryBean query,
                                             @RequestParam(required = false) String name,
                                             @RequestParam(required = false) Integer status) {
        PageResultBean<SysRole> result = roleService.findAll(query, name, status);
        List<RoleVO> voList = result.getList().stream()
                .map(RoleVO::from).collect(Collectors.toList());
        PageResultBean<RoleVO> voResult = new PageResultBean<>();
        voResult.setCount(result.getCount());
        voResult.setList(voList);
        return R.ok(voResult);
    }

    @GetMapping("/{id}/menus")
    @PreAuthorize("hasAuthority('perm:sys:role:list')")
    public R<List<Long>> findMenuIds(@PathVariable Long id) {
        return R.ok(roleService.findMenuIds(id));
    }

    @PutMapping("/{id}/menus")
    @PreAuthorize("hasAuthority('perm:sys:role:update')")
    @OperLog(title = "角色授权", module = "grant")
    public R<RoleVO> assignMenus(@PathVariable Long id, @RequestBody List<Long> menuIds) {
        return R.ok(RoleVO.from(grantService.replaceRoleMenus(id, menuIds)));
    }
}
