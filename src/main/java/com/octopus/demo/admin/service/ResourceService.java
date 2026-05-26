package com.octopus.demo.admin.service;

import com.octopus.demo.admin.dao.ResourceDao;
import com.octopus.demo.admin.entity.SysResource;
import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ResourceService {
    private final ResourceDao resourceDao;

    public ResourceService(ResourceDao resourceDao) {
        this.resourceDao = resourceDao;
    }

    public SysResource create(SysResource resource) {
        return resourceDao.save(resource);
    }

    public SysResource update(SysResource resource) {
        return resourceDao.save(resource);
    }

    public void delete(Long id) {
        resourceDao.deleteById(id);
    }

    public SysResource findById(Long id) {
        return resourceDao.findById(id).orElse(null);
    }

    public List<SysResource> findAll() {
        return resourceDao.findAll();
    }

    public PageResultBean<SysResource> findAll(PageQueryBean query) {
        return resourceDao.findAll(query);
    }
}
