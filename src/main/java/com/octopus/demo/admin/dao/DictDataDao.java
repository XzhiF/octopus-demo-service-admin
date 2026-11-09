package com.octopus.demo.admin.dao;

import com.octopus.demo.admin.entity.SysDictData;

import java.util.List;
import java.util.Optional;

public interface DictDataDao {
    SysDictData save(SysDictData data);
    Optional<SysDictData> findById(Long id);
    List<SysDictData> findByType(String dictType);
    List<SysDictData> findEnabledByType(String dictType);
    void deleteById(Long id);
    long countByType(String dictType);
}
