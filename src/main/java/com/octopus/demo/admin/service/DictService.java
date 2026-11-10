package com.octopus.demo.admin.service;

import com.octopus.demo.admin.dao.DictDataDao;
import com.octopus.demo.admin.dao.DictTypeDao;
import com.octopus.demo.admin.entity.SysDictData;
import com.octopus.demo.admin.entity.SysDictType;
import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class DictService {
    private static final Pattern DICT_TYPE_PATTERN = Pattern.compile("^[a-z0-9_]+$");

    private final DictTypeDao dictTypeDao;
    private final DictDataDao dictDataDao;

    public DictService(DictTypeDao dictTypeDao, DictDataDao dictDataDao) {
        this.dictTypeDao = dictTypeDao;
        this.dictDataDao = dictDataDao;
    }

    public PageResultBean<SysDictType> findTypes(PageQueryBean query, String dictType, String dictName) {
        return dictTypeDao.findAll(query, dictType, dictName);
    }

    public SysDictType createType(SysDictType type) {
        type.setId(null);
        validateType(type, null);
        if (type.getStatus() == null) {
            type.setStatus(1);
        }
        return dictTypeDao.save(type);
    }

    public SysDictType updateType(Long id, SysDictType type) {
        SysDictType existing = requireTypeById(id);
        validateType(type, id);
        if (!existing.getDictType().equals(type.getDictType())
                && dictDataDao.countByType(existing.getDictType()) > 0) {
            throw new IllegalArgumentException("该字典类型下仍有字典项，无法修改类型编码");
        }
        type.setId(id);
        return dictTypeDao.save(type);
    }

    public void deleteType(Long id) {
        SysDictType existing = requireTypeById(id);
        long items = dictDataDao.countByType(existing.getDictType());
        if (items > 0) {
            throw new IllegalArgumentException("该字典类型下仍有 " + items + " 个字典项，无法删除");
        }
        dictTypeDao.deleteById(id);
    }

    public List<SysDictData> findData(String dictType) {
        if (dictType == null || dictType.isBlank()) {
            return List.of();
        }
        return dictDataDao.findByType(dictType);
    }

    public List<SysDictData> findEnabledData(String dictType) {
        return dictDataDao.findEnabledByType(dictType);
    }

    public SysDictData createData(SysDictData data) {
        data.setId(null);
        validateData(data);
        if (data.getSort() == null) {
            data.setSort(0);
        }
        if (data.getStatus() == null) {
            data.setStatus(1);
        }
        return dictDataDao.save(data);
    }

    public SysDictData updateData(Long id, SysDictData data) {
        dictDataDao.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("字典项不存在"));
        validateData(data);
        data.setId(id);
        return dictDataDao.save(data);
    }

    public void deleteData(Long id) {
        dictDataDao.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("字典项不存在"));
        dictDataDao.deleteById(id);
    }

    private SysDictType requireTypeById(Long id) {
        return dictTypeDao.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("字典类型不存在"));
    }

    private void validateType(SysDictType type, Long selfId) {
        if (type.getDictName() == null || type.getDictName().isBlank()) {
            throw new IllegalArgumentException("字典名称不能为空");
        }
        String dictType = type.getDictType();
        if (dictType == null || !DICT_TYPE_PATTERN.matcher(dictType).matches()) {
            throw new IllegalArgumentException("字典类型编码只能包含小写字母、数字和下划线");
        }
        validateStatus(type.getStatus());
        dictTypeDao.findByType(dictType)
                .filter(other -> selfId == null || !selfId.equals(other.getId()))
                .ifPresent(other -> {
                    throw new IllegalArgumentException("字典类型已存在");
                });
    }

    private void validateData(SysDictData data) {
        String dictType = data.getDictType();
        if (dictType == null || dictType.isBlank()) {
            throw new IllegalArgumentException("字典类型不能为空");
        }
        if (dictTypeDao.findByType(dictType).isEmpty()) {
            throw new IllegalArgumentException("字典类型不存在");
        }
        if (data.getDictLabel() == null || data.getDictLabel().isBlank()) {
            throw new IllegalArgumentException("字典标签不能为空");
        }
        if (data.getDictValue() == null || data.getDictValue().isBlank()) {
            throw new IllegalArgumentException("字典键值不能为空");
        }
        validateStatus(data.getStatus());
    }

    private void validateStatus(Integer status) {
        if (status != null && status != 0 && status != 1) {
            throw new IllegalArgumentException("状态值只能为 0 或 1");
        }
    }
}
