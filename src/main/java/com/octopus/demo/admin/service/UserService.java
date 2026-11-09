package com.octopus.demo.admin.service;

import com.octopus.demo.admin.dao.UserDao;
import com.octopus.demo.admin.dao.RoleDao;
import com.octopus.demo.admin.entity.SysUser;
import com.octopus.demo.admin.entity.SysRole;
import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
import com.octopus.demo.common.util.PasswordUtils;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {
    private final UserDao userDao;
    private final RoleDao roleDao;

    public UserService(UserDao userDao, RoleDao roleDao) {
        this.userDao = userDao;
        this.roleDao = roleDao;
    }

    public SysUser create(SysUser user) {
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new IllegalArgumentException("password is required");
        }
        user.setPassword(PasswordUtils.encode(user.getPassword()));
        return userDao.save(user);
    }

    public SysUser update(SysUser user) {
        String rawPassword = user.getPassword();
        // Blank means "keep the stored hash": the mapper COALESCEs a null password_hash to itself.
        user.setPassword(rawPassword == null || rawPassword.isBlank()
                ? null : PasswordUtils.encode(rawPassword));
        return userDao.save(user);
    }

    public void delete(Long id) {
        userDao.deleteById(id);
    }

    public SysUser findById(Long id) {
        return userDao.findById(id).orElse(null);
    }

    public List<SysUser> findAll() {
        return userDao.findAll();
    }

    public PageResultBean<SysUser> findAll(PageQueryBean query) {
        return userDao.findAll(query);
    }

    public SysUser assignRoles(Long userId, List<Long> roleIds) {
        SysUser user = userDao.findById(userId).orElse(null);
        if (user == null) return null;
        List<SysRole> roles = roleIds.stream()
            .map(roleDao::findById)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
        user.setRoles(roles);
        return userDao.save(user);
    }
}
