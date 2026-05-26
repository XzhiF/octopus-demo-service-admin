package com.octopus.demo.admin.dao.impl;

import com.octopus.demo.admin.dao.UserDao;
import com.octopus.demo.admin.entity.SysUser;
import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
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

    @Override
    public PageResultBean<SysUser> findAll(PageQueryBean query) {
        List<SysUser> all = new ArrayList<>(store.values());
        long count = all.size();
        long fromIndexLong = (long) (query.getPage() - 1) * query.getSize();
        if (fromIndexLong >= count || fromIndexLong > Integer.MAX_VALUE) {
            PageResultBean<SysUser> result = new PageResultBean<>();
            result.setCount(count);
            result.setList(List.of());
            return result;
        }
        int fromIndex = (int) fromIndexLong;
        int toIndex = Math.min(fromIndex + query.getSize(), all.size());
        List<SysUser> page = new ArrayList<>(all.subList(fromIndex, toIndex));
        PageResultBean<SysUser> result = new PageResultBean<>();
        result.setCount(count);
        result.setList(page);
        return result;
    }
}
