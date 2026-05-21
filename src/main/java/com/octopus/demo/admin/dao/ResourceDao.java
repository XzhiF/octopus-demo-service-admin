package com.octopus.demo.admin.dao;

import com.octopus.demo.admin.entity.SysResource;
import java.util.Optional;

public interface ResourceDao {
    SysResource save(SysResource resource);
    Optional<SysResource> findById(Long id);
    Optional<SysResource> findByCode(String code);
    java.util.List<SysResource> findAll();
    void deleteById(Long id);
}