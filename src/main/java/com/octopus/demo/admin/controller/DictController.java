package com.octopus.demo.admin.controller;

import com.octopus.demo.admin.annotation.OperLog;
import com.octopus.demo.admin.entity.SysDictData;
import com.octopus.demo.admin.entity.SysDictType;
import com.octopus.demo.admin.service.DictService;
import com.octopus.demo.admin.vo.DictDataVO;
import com.octopus.demo.admin.vo.DictTypeVO;
import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
import com.octopus.demo.common.bean.R;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dicts")
public class DictController {
    private final DictService dictService;

    public DictController(DictService dictService) {
        this.dictService = dictService;
    }

    @GetMapping("/types")
    @PreAuthorize("hasAuthority('perm:sys:dict:list')")
    public R<PageResultBean<DictTypeVO>> findTypes(PageQueryBean query,
                                                   @RequestParam(required = false) String dictType,
                                                   @RequestParam(required = false) String dictName) {
        PageResultBean<SysDictType> result = dictService.findTypes(query, dictType, dictName);
        PageResultBean<DictTypeVO> voResult = new PageResultBean<>();
        voResult.setCount(result.getCount());
        voResult.setList(result.getList().stream().map(DictTypeVO::from).collect(Collectors.toList()));
        return R.ok(voResult);
    }

    @PostMapping("/types")
    @PreAuthorize("hasAuthority('perm:sys:dict:add')")
    @OperLog(title = "新增字典类型", module = "dict")
    public R<DictTypeVO> createType(@RequestBody SysDictType type) {
        return R.ok(DictTypeVO.from(dictService.createType(type)));
    }

    @PutMapping("/types/{id}")
    @PreAuthorize("hasAuthority('perm:sys:dict:edit')")
    @OperLog(title = "修改字典类型", module = "dict")
    public R<DictTypeVO> updateType(@PathVariable Long id, @RequestBody SysDictType type) {
        return R.ok(DictTypeVO.from(dictService.updateType(id, type)));
    }

    @DeleteMapping("/types/{id}")
    @PreAuthorize("hasAuthority('perm:sys:dict:delete')")
    @OperLog(title = "删除字典类型", module = "dict")
    public R<Void> deleteType(@PathVariable Long id) {
        dictService.deleteType(id);
        return R.ok();
    }

    @GetMapping("/data")
    @PreAuthorize("hasAuthority('perm:sys:dict:list')")
    public R<List<SysDictData>> findData(@RequestParam(required = false) String dictType) {
        return R.ok(dictService.findData(dictType));
    }

    /** Dropdown feed: enabled items only, ordered by sort. Login required, no admin permission. */
    @GetMapping("/data/{dictType}")
    public R<List<DictDataVO>> findEnabledData(@PathVariable String dictType) {
        return R.ok(dictService.findEnabledData(dictType).stream()
                .map(DictDataVO::from).collect(Collectors.toList()));
    }

    @PostMapping("/data")
    @PreAuthorize("hasAuthority('perm:sys:dict:add')")
    @OperLog(title = "新增字典数据", module = "dict")
    public R<SysDictData> createData(@RequestBody SysDictData data) {
        return R.ok(dictService.createData(data));
    }

    @PutMapping("/data/{id}")
    @PreAuthorize("hasAuthority('perm:sys:dict:edit')")
    @OperLog(title = "修改字典数据", module = "dict")
    public R<SysDictData> updateData(@PathVariable Long id, @RequestBody SysDictData data) {
        return R.ok(dictService.updateData(id, data));
    }

    @DeleteMapping("/data/{id}")
    @PreAuthorize("hasAuthority('perm:sys:dict:delete')")
    @OperLog(title = "删除字典数据", module = "dict")
    public R<Void> deleteData(@PathVariable Long id) {
        dictService.deleteData(id);
        return R.ok();
    }
}
