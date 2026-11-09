package com.octopus.demo.admin.dao;

import com.octopus.demo.admin.entity.SysDictType;
import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;

import java.util.Optional;

public interface DictTypeDao {
    SysDictType save(SysDictType type);
    Optional<SysDictType> findById(Long id);
    Optional<SysDictType> findByType(String dictType);
    PageResultBean<SysDictType> findAll(PageQueryBean query, String dictType, String dictName);
    void deleteById(Long id);
}
