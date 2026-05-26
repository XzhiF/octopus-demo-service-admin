package com.octopus.demo.admin.controller;

import com.octopus.demo.admin.entity.SysResource;
import com.octopus.demo.admin.service.ResourceService;
import com.octopus.demo.admin.vo.ResourceVO;
import com.octopus.demo.common.bean.R;
import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {
    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @PostMapping
    public R<ResourceVO> create(@RequestBody SysResource resource) {
        return R.ok(ResourceVO.from(resourceService.create(resource)));
    }

    @PutMapping
    public R<ResourceVO> update(@RequestBody SysResource resource) {
        return R.ok(ResourceVO.from(resourceService.update(resource)));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        resourceService.delete(id);
        return R.ok();
    }

    @GetMapping("/{id}")
    public R<ResourceVO> findById(@PathVariable Long id) {
        SysResource resource = resourceService.findById(id);
        if (resource == null) return R.fail(404, "resource not found");
        return R.ok(ResourceVO.from(resource));
    }

    @GetMapping
    public R<PageResultBean<ResourceVO>> findAll(PageQueryBean query) {
        PageResultBean<SysResource> result = resourceService.findAll(query);
        List<ResourceVO> voList = result.getList().stream()
                .map(ResourceVO::from).collect(Collectors.toList());
        PageResultBean<ResourceVO> voResult = new PageResultBean<>();
        voResult.setCount(result.getCount());
        voResult.setList(voList);
        return R.ok(voResult);
    }
}
