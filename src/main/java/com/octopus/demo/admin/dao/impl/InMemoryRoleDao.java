package com.octopus.demo.admin.dao.impl;

import com.octopus.demo.admin.dao.RoleDao;
import com.octopus.demo.admin.entity.SysRole;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryRoleDao implements RoleDao {
    private final Map<Long, SysRole> store = new HashMap<>();
    private final AtomicLong idSeq = new AtomicLong(1);

    @Override
    public SysRole save(SysRole role) {
        if (role.getId() == null) {
            role.setId(idSeq.getAndIncrement());
        }
        store.put(role.getId(), role);
        return role;
    }

    @Override
    public Optional<SysRole> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<SysRole> findByCode(String code) {
        return store.values().stream().filter(r -> r.getCode().equals(code)).findFirst();
    }

    @Override
    public List<SysRole> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void deleteById(Long id) {
        store.remove(id);
    }
}