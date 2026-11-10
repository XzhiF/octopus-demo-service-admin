package com.octopus.demo.admin.controller;

import com.octopus.demo.admin.security.AuthContext;
import com.octopus.demo.admin.service.AuthService;
import com.octopus.demo.admin.vo.AuthVO;
import com.octopus.demo.admin.vo.CurrentUserVO;
import com.octopus.demo.admin.vo.MenuVO;
import com.octopus.demo.admin.web.ClientIpResolver;
import com.octopus.demo.common.bean.R;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<R<AuthVO>> login(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String ip = ClientIpResolver.resolve(request);
        String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
        return authService.authenticate(body.get("username"), body.get("password"), ip, userAgent)
                .map(context -> {
                    String token = authService.issueToken(context.userId());
                    return ResponseEntity.ok(R.ok(AuthVO.of(token, authService.expirationSeconds(), context)));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(R.fail(401, "用户名或密码错误")));
    }

    @GetMapping("/me")
    public R<CurrentUserVO> me(Authentication authentication) {
        return R.ok(CurrentUserVO.from(principal(authentication)));
    }

    @PostMapping("/logout")
    public R<Void> logout() {
        return R.ok();
    }

    @GetMapping("/menus")
    public R<List<MenuVO>> menus(Authentication authentication) {
        return R.ok(authService.menuTree(principal(authentication).userId()));
    }

    private AuthContext principal(Authentication authentication) {
        return (AuthContext) authentication.getPrincipal();
    }
}
