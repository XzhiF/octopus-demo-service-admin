package com.octopus.demo.admin.dao;

import com.octopus.demo.admin.entity.SysResource;
import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
import java.util.Optional;

public interface ResourceDao {
    SysResource save(SysResource resource);
    Optional<SysResource> findById(Long id);
    Optional<SysResource> findByCode(String code);
    java.util.List<SysResource> findAll();
    void deleteById(Long id);
    PageResultBean<SysResource> findAll(PageQueryBean query);
}
