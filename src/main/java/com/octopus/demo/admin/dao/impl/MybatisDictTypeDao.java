package com.octopus.demo.admin.dao.impl;

import com.octopus.demo.admin.dao.DictTypeDao;
import com.octopus.demo.admin.entity.SysDictType;
import com.octopus.demo.admin.mapper.DictTypeMapper;
import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MybatisDictTypeDao implements DictTypeDao {

    private final DictTypeMapper dictTypeMapper;

    public MybatisDictTypeDao(DictTypeMapper dictTypeMapper) {
        this.dictTypeMapper = dictTypeMapper;
    }

    @Override
    public SysDictType save(SysDictType type) {
        if (type.getId() == null) {
            dictTypeMapper.insert(type);
        } else {
            dictTypeMapper.update(type);
        }
        return type;
    }

    @Override
    public Optional<SysDictType> findById(Long id) {
        return Optional.ofNullable(dictTypeMapper.findById(id));
    }

    @Override
    public Optional<SysDictType> findByType(String dictType) {
        return Optional.ofNullable(dictTypeMapper.findByType(dictType));
    }

    @Override
    public PageResultBean<SysDictType> findAll(PageQueryBean query, String dictType, String dictName) {
        long total = dictTypeMapper.count(dictType, dictName);
        long offset = (long) (query.getPage() - 1) * query.getSize();
        PageResultBean<SysDictType> result = new PageResultBean<>();
        result.setCount(total);
        if (offset >= total) {
            result.setList(List.of());
            return result;
        }
        result.setList(dictTypeMapper.findPage(offset, query.getSize(), dictType, dictName));
        return result;
    }

    @Override
    public void deleteById(Long id) {
        dictTypeMapper.deleteById(id);
    }
}
