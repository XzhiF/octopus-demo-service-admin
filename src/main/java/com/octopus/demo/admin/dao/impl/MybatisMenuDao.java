package com.octopus.demo.admin.dao.impl;

import com.octopus.demo.admin.dao.ResourceDao;
import com.octopus.demo.admin.entity.SysResource;
import com.octopus.demo.admin.mapper.MenuMapper;
import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MybatisMenuDao implements ResourceDao {

    private final MenuMapper menuMapper;

    public MybatisMenuDao(MenuMapper menuMapper) {
        this.menuMapper = menuMapper;
    }

    @Override
    public SysResource save(SysResource resource) {
        if (resource.getId() == null) {
            menuMapper.insert(resource);
        } else {
            menuMapper.update(resource);
        }
        return resource;
    }

    @Override
    public Optional<SysResource> findById(Long id) {
        return Optional.ofNullable(menuMapper.findById(id));
    }

    @Override
    public Optional<SysResource> findByCode(String code) {
        return Optional.ofNullable(menuMapper.findByCode(code));
    }

    @Override
    public List<SysResource> findAll() {
        return menuMapper.findAll();
    }

    @Override
    public void deleteById(Long id) {
        menuMapper.deleteById(id);
    }

    @Override
    public PageResultBean<SysResource> findAll(PageQueryBean query) {
        long total = menuMapper.count();
        long offset = (long) (query.getPage() - 1) * query.getSize();
        PageResultBean<SysResource> result = new PageResultBean<>();
        result.setCount(total);
        if (offset >= total) {
            result.setList(List.of());
            return result;
        }
        result.setList(menuMapper.findPage(offset, query.getSize()));
        return result;
    }

    @Override
    public List<SysResource> findVisibleByUserId(Long userId) {
        return menuMapper.findVisibleByUserId(userId);
    }
}
