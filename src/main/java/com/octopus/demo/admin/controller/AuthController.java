package com.octopus.demo.admin.controller;

import com.octopus.demo.admin.entity.SysUser;
import com.octopus.demo.admin.entity.SysResource;
import com.octopus.demo.admin.service.AuthService;
import com.octopus.demo.admin.vo.AuthVO;
import com.octopus.demo.admin.vo.UserVO;
import com.octopus.demo.admin.vo.ResourceVO;
import com.octopus.demo.common.bean.R;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public R<AuthVO> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String token = authService.login(username, password);
        if (token == null) {
            return R.fail(401, "invalid username or password");
        }
        SysUser user = authService.getCurrentUser(token);
        return R.ok(AuthVO.of(token, UserVO.from(user)));
    }

    @PostMapping("/logout")
    public R<Void> logout(@RequestHeader("X-Token") String token) {
        authService.logout(token);
        return R.ok();
    }

    @GetMapping("/current")
    public R<UserVO> current(@RequestHeader("X-Token") String token) {
        SysUser user = authService.getCurrentUser(token);
        if (user == null) return R.fail(401, "not logged in");
        return R.ok(UserVO.from(user));
    }

    @GetMapping("/resources")
    public R<List<ResourceVO>> resources(@RequestHeader("X-Token") String token) {
        List<SysResource> list = authService.getUserResources(token);
        if (list.isEmpty()) return R.fail(401, "not logged in");
        return R.ok(list.stream().map(ResourceVO::from).collect(Collectors.toList()));
    }
}
