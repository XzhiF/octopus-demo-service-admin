package com.octopus.demo.admin.service;

import com.octopus.demo.admin.dao.RoleDao;
import com.octopus.demo.admin.dao.ResourceDao;
import com.octopus.demo.admin.entity.SysRole;
import com.octopus.demo.admin.entity.SysResource;
import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RoleService {
    private final RoleDao roleDao;
    private final ResourceDao resourceDao;

    public RoleService(RoleDao roleDao, ResourceDao resourceDao) {
        this.roleDao = roleDao;
        this.resourceDao = resourceDao;
    }

    public SysRole create(SysRole role) {
        return roleDao.save(role);
    }

    public SysRole update(SysRole role) {
        return roleDao.save(role);
    }

    public void delete(Long id) {
        roleDao.deleteById(id);
    }

    public SysRole findById(Long id) {
        return roleDao.findById(id).orElse(null);
    }

    public List<SysRole> findAll() {
        return roleDao.findAll();
    }

    public PageResultBean<SysRole> findAll(PageQueryBean query) {
        return roleDao.findAll(query);
    }

    public SysRole assignResources(Long roleId, List<Long> resourceIds) {
        SysRole role = roleDao.findById(roleId).orElse(null);
        if (role == null) return null;
        List<SysResource> resources = resourceIds.stream()
            .map(resourceDao::findById)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
        role.setResources(resources);
        return roleDao.save(role);
    }
}
