package com.octopus.demo.admin.controller;

import com.octopus.demo.admin.entity.SysOperLog;
import com.octopus.demo.admin.service.OperLogService;
import com.octopus.demo.admin.vo.OperLogVO;
import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
import com.octopus.demo.common.bean.R;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/logs/oper")
public class OperLogController {

    private final OperLogService operLogService;

    public OperLogController(OperLogService operLogService) {
        this.operLogService = operLogService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('perm:sys:operlog:list')")
    public R<PageResultBean<OperLogVO>> findAll(
            PageQueryBean query,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        PageResultBean<SysOperLog> result = operLogService.findAll(query, operator, module, status, from, to);
        PageResultBean<OperLogVO> voResult = new PageResultBean<>();
        voResult.setCount(result.getCount());
        voResult.setList(result.getList().stream().map(OperLogVO::from).collect(Collectors.toList()));
        return R.ok(voResult);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('perm:sys:operlog:list')")
    public R<OperLogVO> findById(@PathVariable Long id) {
        SysOperLog entry = operLogService.findById(id);
        if (entry == null) {
            return R.fail(404, "oper log not found");
        }
        return R.ok(OperLogVO.from(entry));
    }
}
