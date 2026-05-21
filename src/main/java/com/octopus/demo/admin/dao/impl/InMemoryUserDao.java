package com.octopus.demo.admin.dao.impl;

import com.octopus.demo.admin.dao.UserDao;
import com.octopus.demo.admin.entity.SysUser;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryUserDao implements UserDao {
    private final Map<Long, SysUser> store = new HashMap<>();
    private final AtomicLong idSeq = new AtomicLong(1);

    @Override
    public SysUser save(SysUser user) {
        if (user.getId() == null) {
            user.setId(idSeq.getAndIncrement());
        }
        store.put(user.getId(), user);
        return user;
    }

    @Override
    public Optional<SysUser> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<SysUser> findByUsername(String username) {
        return store.values().stream().filter(u -> u.getUsername().equals(username)).findFirst();
    }

    @Override
    public List<SysUser> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void deleteById(Long id) {
        store.remove(id);
    }
}