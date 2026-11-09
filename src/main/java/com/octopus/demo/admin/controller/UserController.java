package com.octopus.demo.admin.controller;

import com.octopus.demo.admin.dto.PasswordRequest;
import com.octopus.demo.admin.dto.StatusRequest;
import com.octopus.demo.admin.dto.UserSaveRequest;
import com.octopus.demo.admin.entity.SysUser;
import com.octopus.demo.admin.security.AuthContext;
import com.octopus.demo.admin.service.GrantService;
import com.octopus.demo.admin.service.UserService;
import com.octopus.demo.admin.vo.RoleVO;
import com.octopus.demo.admin.vo.UserVO;
import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
import com.octopus.demo.common.bean.R;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final GrantService grantService;

    public UserController(UserService userService, GrantService grantService) {
        this.userService = userService;
        this.grantService = grantService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('perm:sys:user:add')")
    public R<UserVO> create(@RequestBody UserSaveRequest request) {
        return R.ok(UserVO.from(userService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('perm:sys:user:update')")
    public R<UserVO> update(@PathVariable Long id, @RequestBody UserSaveRequest request) {
        return R.ok(UserVO.from(userService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('perm:sys:user:delete')")
    public R<Void> delete(@PathVariable Long id, Authentication authentication) {
        userService.delete(id, currentUserId(authentication));
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
    public R<PageResultBean<UserVO>> findAll(PageQueryBean query,
                                             @RequestParam(required = false) String username,
                                             @RequestParam(required = false) Integer status) {
        PageResultBean<SysUser> result = userService.findAll(query, username, status);
        List<UserVO> voList = result.getList().stream()
                .map(UserVO::from).collect(Collectors.toList());
        PageResultBean<UserVO> voResult = new PageResultBean<>();
        voResult.setCount(result.getCount());
        voResult.setList(voList);
        return R.ok(voResult);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('perm:sys:user:update')")
    public R<UserVO> updateStatus(@PathVariable Long id, @RequestBody StatusRequest request) {
        return R.ok(UserVO.from(userService.updateStatus(id, request.status())));
    }

    @PutMapping("/{id}/password")
    @PreAuthorize("hasAuthority('perm:sys:user:update')")
    public R<Void> resetPassword(@PathVariable Long id, @RequestBody PasswordRequest request) {
        userService.resetPassword(id, request.password());
        return R.ok();
    }

    @GetMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('perm:sys:user:update')")
    public R<List<RoleVO>> roles(@PathVariable Long id) {
        SysUser user = userService.findById(id);
        if (user == null) return R.fail(404, "user not found");
        return R.ok(user.getRoles().stream().map(RoleVO::from).collect(Collectors.toList()));
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('perm:sys:user:update')")
    public R<UserVO> assignRoles(@PathVariable Long id, @RequestBody List<Long> roleIds) {
        return R.ok(UserVO.from(grantService.replaceUserRoles(id, roleIds)));
    }

    private Long currentUserId(Authentication authentication) {
        return ((AuthContext) authentication.getPrincipal()).userId();
    }
}
