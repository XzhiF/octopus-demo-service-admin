package com.octopus.demo.admin.dao.impl;

import com.octopus.demo.admin.dao.UserDao;
import com.octopus.demo.admin.entity.SysRole;
import com.octopus.demo.admin.entity.SysUser;
import com.octopus.demo.admin.mapper.UserMapper;
import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MybatisUserDao implements UserDao {

    private final UserMapper userMapper;

    public MybatisUserDao(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public SysUser save(SysUser user) {
        if (user.getId() == null) {
            userMapper.insert(user);
        } else {
            userMapper.update(user);
        }
        bindRoles(user);
        return user;
    }

    private void bindRoles(SysUser user) {
        if (user.getId() == null || user.getRoles() == null || user.getRoles().isEmpty()) {
            return;
        }
        userMapper.deleteUserRoles(user.getId());
        for (SysRole role : user.getRoles()) {
            if (role != null && role.getId() != null) {
                userMapper.insertUserRole(user.getId(), role.getId());
            }
        }
    }

    @Override
    public Optional<SysUser> findById(Long id) {
        return Optional.ofNullable(userMapper.findById(id));
    }

    @Override
    public Optional<SysUser> findByUsername(String username) {
        return Optional.ofNullable(userMapper.findByUsername(username));
    }

    @Override
    public List<SysUser> findAll() {
        return userMapper.findAll();
    }

    @Override
    public void deleteById(Long id) {
        userMapper.deleteUserRoles(id);
        userMapper.deleteById(id);
    }

    @Override
    public PageResultBean<SysUser> findAll(PageQueryBean query) {
        long total = userMapper.count();
        long offset = (long) (query.getPage() - 1) * query.getSize();
        PageResultBean<SysUser> result = new PageResultBean<>();
        result.setCount(total);
        if (offset >= total) {
            result.setList(List.of());
            return result;
        }
        result.setList(userMapper.findPage(offset, query.getSize()));
        return result;
    }
}
