package com.octopus.demo.admin.dao.impl;

import com.octopus.demo.admin.dao.UserDao;
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
        return user;
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
    public PageResultBean<SysUser> findAll(PageQueryBean query, String username, Integer status) {
        long total = userMapper.count(username, status);
        long offset = (long) (query.getPage() - 1) * query.getSize();
        PageResultBean<SysUser> result = new PageResultBean<>();
        result.setCount(total);
        if (offset >= total) {
            result.setList(List.of());
            return result;
        }
        List<SysUser> users = userMapper.findPage(offset, query.getSize(), username, status);
        users.forEach(user -> user.setRoles(userMapper.findRolesByUserId(user.getId())));
        result.setList(users);
        return result;
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        userMapper.updateStatus(id, status);
    }

    @Override
    public void updatePassword(Long id, String passwordHash) {
        userMapper.updatePassword(id, passwordHash);
    }

    @Override
    public void replaceRoles(Long userId, List<Long> roleIds) {
        userMapper.deleteUserRoles(userId);
        if (roleIds == null) {
            return;
        }
        for (Long roleId : roleIds) {
            if (roleId != null) {
                userMapper.insertUserRole(userId, roleId);
            }
        }
    }
}
