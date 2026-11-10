package com.octopus.demo.admin.controller;

import com.octopus.demo.admin.entity.SysLoginLog;
import com.octopus.demo.admin.service.LoginLogService;
import com.octopus.demo.admin.vo.LoginLogVO;
import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
import com.octopus.demo.common.bean.R;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/logs")
public class LoginLogController {

    private final LoginLogService loginLogService;

    public LoginLogController(LoginLogService loginLogService) {
        this.loginLogService = loginLogService;
    }

    @GetMapping("/login")
    @PreAuthorize("hasAuthority('perm:sys:loginlog:list')")
    public R<PageResultBean<LoginLogVO>> findAll(
            PageQueryBean query,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        PageResultBean<SysLoginLog> result = loginLogService.findAll(query, username, status, from, to);
        PageResultBean<LoginLogVO> voResult = new PageResultBean<>();
        voResult.setCount(result.getCount());
        voResult.setList(result.getList().stream().map(LoginLogVO::from).collect(Collectors.toList()));
        return R.ok(voResult);
    }
}
