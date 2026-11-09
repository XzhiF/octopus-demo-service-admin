package com.octopus.demo.admin.dao.impl;

import com.octopus.demo.admin.dao.RoleDao;
import com.octopus.demo.admin.entity.SysResource;
import com.octopus.demo.admin.entity.SysRole;
import com.octopus.demo.admin.mapper.RoleMapper;
import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MybatisRoleDao implements RoleDao {

    private final RoleMapper roleMapper;

    public MybatisRoleDao(RoleMapper roleMapper) {
        this.roleMapper = roleMapper;
    }

    @Override
    public SysRole save(SysRole role) {
        if (role.getId() == null) {
            roleMapper.insert(role);
        } else {
            roleMapper.update(role);
        }
        bindResources(role);
        return role;
    }

    private void bindResources(SysRole role) {
        if (role.getId() == null || role.getResources() == null || role.getResources().isEmpty()) {
            return;
        }
        roleMapper.deleteRoleMenus(role.getId());
        for (SysResource resource : role.getResources()) {
            if (resource != null && resource.getId() != null) {
                roleMapper.insertRoleMenu(role.getId(), resource.getId());
            }
        }
    }

    @Override
    public Optional<SysRole> findById(Long id) {
        return Optional.ofNullable(roleMapper.findById(id));
    }

    @Override
    public Optional<SysRole> findByCode(String code) {
        return Optional.ofNullable(roleMapper.findByCode(code));
    }

    @Override
    public List<SysRole> findAll() {
        return roleMapper.findAll();
    }

    @Override
    public void deleteById(Long id) {
        roleMapper.deleteRoleMenus(id);
        roleMapper.deleteById(id);
    }

    @Override
    public PageResultBean<SysRole> findAll(PageQueryBean query) {
        long total = roleMapper.count();
        long offset = (long) (query.getPage() - 1) * query.getSize();
        PageResultBean<SysRole> result = new PageResultBean<>();
        result.setCount(total);
        if (offset >= total) {
            result.setList(List.of());
            return result;
        }
        result.setList(roleMapper.findPage(offset, query.getSize()));
        return result;
    }
}
