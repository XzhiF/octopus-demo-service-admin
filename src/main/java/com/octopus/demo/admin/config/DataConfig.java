package com.octopus.demo.admin.config;

import com.octopus.demo.admin.dao.impl.InMemoryUserDao;
import com.octopus.demo.admin.dao.impl.InMemoryRoleDao;
import com.octopus.demo.admin.dao.impl.InMemoryResourceDao;
import com.octopus.demo.admin.dao.UserDao;
import com.octopus.demo.admin.dao.RoleDao;
import com.octopus.demo.admin.dao.ResourceDao;
import com.octopus.demo.admin.entity.SysUser;
import com.octopus.demo.admin.entity.SysRole;
import com.octopus.demo.admin.entity.SysResource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataConfig {

    @Bean
    public UserDao userDao() {
        return new InMemoryUserDao();
    }

    @Bean
    public RoleDao roleDao() {
        return new InMemoryRoleDao();
    }

    @Bean
    public ResourceDao resourceDao() {
        return new InMemoryResourceDao();
    }

    @Bean
    public DataInitializer dataInitializer(UserDao userDao, RoleDao roleDao, ResourceDao resourceDao) {
        return new DataInitializer(userDao, roleDao, resourceDao);
    }

    static class DataInitializer {
        DataInitializer(UserDao userDao, RoleDao roleDao, ResourceDao resourceDao) {
            initResources(resourceDao);
            initRoles(roleDao, resourceDao);
            initUsers(userDao, roleDao);
        }

        private void initResources(ResourceDao dao) {
            dao.save(new SysResource(null, "用户管理", "user:manage", "menu"));
            dao.save(new SysResource(null, "角色管理", "role:manage", "menu"));
            dao.save(new SysResource(null, "资源管理", "resource:manage", "menu"));
        }

        private void initRoles(RoleDao dao, ResourceDao resDao) {
            SysRole admin = dao.save(new SysRole(null, "管理员", "admin"));
            SysRole user = dao.save(new SysRole(null, "普通用户", "user"));

            admin.setResources(resDao.findAll());
            dao.save(admin);

            SysResource userRes = resDao.findByCode("user:manage").orElse(null);
            if (userRes != null) {
                user.setResources(java.util.Collections.singletonList(userRes));
                dao.save(user);
            }
        }

        private void initUsers(UserDao dao, RoleDao roleDao) {
            SysUser admin = dao.save(new SysUser(null, "admin", "123456"));
            admin.setRoles(roleDao.findAll());
            dao.save(admin);

            SysRole userRole = roleDao.findByCode("user").orElse(null);
            SysUser guest = dao.save(new SysUser(null, "guest", "123456"));
            if (userRole != null) {
                guest.setRoles(java.util.Collections.singletonList(userRole));
                dao.save(guest);
            }
        }
    }
}