package com.octopus.demo.admin.dao.impl;

import com.octopus.demo.admin.dao.ResourceDao;
import com.octopus.demo.admin.entity.SysResource;
import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryResourceDao implements ResourceDao {
    private final Map<Long, SysResource> store = new HashMap<>();
    private final AtomicLong idSeq = new AtomicLong(1);

    @Override
    public SysResource save(SysResource resource) {
        if (resource.getId() == null) {
            resource.setId(idSeq.getAndIncrement());
        }
        store.put(resource.getId(), resource);
        return resource;
    }

    @Override
    public Optional<SysResource> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<SysResource> findByCode(String code) {
        return store.values().stream().filter(r -> r.getCode().equals(code)).findFirst();
    }

    @Override
    public List<SysResource> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void deleteById(Long id) {
        store.remove(id);
    }

    @Override
    public PageResultBean<SysResource> findAll(PageQueryBean query) {
        List<SysResource> all = new ArrayList<>(store.values());
        long count = all.size();
        long fromIndexLong = (long) (query.getPage() - 1) * query.getSize();
        if (fromIndexLong >= count || fromIndexLong > Integer.MAX_VALUE) {
            PageResultBean<SysResource> result = new PageResultBean<>();
            result.setCount(count);
            result.setList(List.of());
            return result;
        }
        int fromIndex = (int) fromIndexLong;
        int toIndex = Math.min(fromIndex + query.getSize(), all.size());
        List<SysResource> page = new ArrayList<>(all.subList(fromIndex, toIndex));
        PageResultBean<SysResource> result = new PageResultBean<>();
        result.setCount(count);
        result.setList(page);
        return result;
    }
}
