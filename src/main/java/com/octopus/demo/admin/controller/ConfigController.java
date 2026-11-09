package com.octopus.demo.admin.controller;

import com.octopus.demo.admin.dto.ConfigRequest;
import com.octopus.demo.admin.entity.SysConfig;
import com.octopus.demo.admin.service.ConfigService;
import com.octopus.demo.admin.vo.ConfigVO;
import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
import com.octopus.demo.common.bean.R;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/configs")
public class ConfigController {

    private final ConfigService configService;

    public ConfigController(ConfigService configService) {
        this.configService = configService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('perm:sys:config:list')")
    public R<PageResultBean<ConfigVO>> findAll(PageQueryBean query,
                                               @RequestParam(required = false) String key) {
        PageResultBean<SysConfig> result = configService.findAll(query, key);
        List<ConfigVO> voList = result.getList().stream()
                .map(ConfigVO::from).collect(Collectors.toList());
        PageResultBean<ConfigVO> voResult = new PageResultBean<>();
        voResult.setCount(result.getCount());
        voResult.setList(voList);
        return R.ok(voResult);
    }

    /** Runtime read for any authenticated user; consumers interpret the string value. */
    @GetMapping("/key/{key}")
    public R<String> value(@PathVariable String key) {
        return R.ok(configService.getValue(key));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('perm:sys:config:add')")
    public R<ConfigVO> create(@RequestBody ConfigRequest request) {
        return R.ok(ConfigVO.from(configService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('perm:sys:config:edit')")
    public R<ConfigVO> update(@PathVariable Long id, @RequestBody ConfigRequest request) {
        return R.ok(ConfigVO.from(configService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('perm:sys:config:delete')")
    public R<Void> delete(@PathVariable Long id) {
        configService.delete(id);
        return R.ok();
    }
}
