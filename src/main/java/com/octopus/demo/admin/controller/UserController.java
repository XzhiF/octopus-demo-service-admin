package com.octopus.demo.admin.controller;

import com.octopus.demo.admin.entity.SysUser;
import com.octopus.demo.admin.service.UserService;
import com.octopus.demo.admin.vo.UserVO;
import com.octopus.demo.common.bean.R;
import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('perm:sys:user:add')")
    public R<UserVO> create(@RequestBody SysUser user) {
        return R.ok(UserVO.from(userService.create(user)));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('perm:sys:user:update')")
    public R<UserVO> update(@RequestBody SysUser user) {
        return R.ok(UserVO.from(userService.update(user)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('perm:sys:user:delete')")
    public R<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return R.ok();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('perm:sys:user:list')")
    public R<UserVO> findById(@PathVariable Long id) {
        SysUser user = userService.findById(id);
        if (user == null) return R.fail(404, "user not found");
        return R.ok(UserVO.from(user));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('perm:sys:user:list')")
    public R<PageResultBean<UserVO>> findAll(PageQueryBean query) {
        PageResultBean<SysUser> result = userService.findAll(query);
        List<UserVO> voList = result.getList().stream()
                .map(UserVO::from).collect(Collectors.toList());
        PageResultBean<UserVO> voResult = new PageResultBean<>();
        voResult.setCount(result.getCount());
        voResult.setList(voList);
        return R.ok(voResult);
    }

    @PostMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('perm:sys:user:update')")
    public R<UserVO> assignRoles(@PathVariable Long id, @RequestBody List<Long> roleIds) {
        SysUser user = userService.assignRoles(id, roleIds);
        if (user == null) return R.fail(404, "user not found");
        return R.ok(UserVO.from(user));
    }
}
