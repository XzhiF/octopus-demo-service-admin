package com.octopus.demo.admin.dao.impl;

import com.octopus.demo.admin.dao.DictDataDao;
import com.octopus.demo.admin.entity.SysDictData;
import com.octopus.demo.admin.mapper.DictDataMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MybatisDictDataDao implements DictDataDao {

    private final DictDataMapper dictDataMapper;

    public MybatisDictDataDao(DictDataMapper dictDataMapper) {
        this.dictDataMapper = dictDataMapper;
    }

    @Override
    public SysDictData save(SysDictData data) {
        if (data.getId() == null) {
            dictDataMapper.insert(data);
        } else {
            dictDataMapper.update(data);
        }
        return data;
    }

    @Override
    public Optional<SysDictData> findById(Long id) {
        return Optional.ofNullable(dictDataMapper.findById(id));
    }

    @Override
    public List<SysDictData> findByType(String dictType) {
        return dictDataMapper.findByType(dictType);
    }

    @Override
    public List<SysDictData> findEnabledByType(String dictType) {
        return dictDataMapper.findEnabledByType(dictType);
    }

    @Override
    public void deleteById(Long id) {
        dictDataMapper.deleteById(id);
    }

    @Override
    public long countByType(String dictType) {
        return dictDataMapper.countByType(dictType);
    }
}
