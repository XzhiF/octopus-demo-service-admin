package com.octopus.demo.admin.controller;

import com.octopus.demo.admin.dto.MenuRequest;
import com.octopus.demo.admin.entity.SysResource;
import com.octopus.demo.admin.service.MenuService;
import com.octopus.demo.admin.vo.MenuVO;
import com.octopus.demo.common.bean.R;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/menus")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('perm:sys:menu:list')")
    public R<List<MenuVO>> tree() {
        return R.ok(menuService.tree());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('perm:sys:menu:add')")
    public R<MenuVO> create(@RequestBody MenuRequest request) {
        SysResource created = menuService.create(request);
        return R.ok(MenuVO.from(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('perm:sys:menu:update')")
    public R<MenuVO> update(@PathVariable Long id, @RequestBody MenuRequest request) {
        SysResource updated = menuService.update(id, request);
        return R.ok(MenuVO.from(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('perm:sys:menu:delete')")
    public R<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return R.ok();
    }
}
