package com.octopus.demo.admin.service;

import com.octopus.demo.admin.dao.ResourceDao;
import com.octopus.demo.admin.dto.MenuRequest;
import com.octopus.demo.admin.entity.SysResource;
import com.octopus.demo.admin.vo.MenuVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class MenuService {

    private static final Set<String> TYPES = Set.of("catalog", "menu", "button");

    private final ResourceDao resourceDao;

    public MenuService(ResourceDao resourceDao) {
        this.resourceDao = resourceDao;
    }

    /** Whole sys_menu forest, buttons included, siblings ordered by sort then id. */
    public List<MenuVO> tree() {
        List<SysResource> menus = resourceDao.findAll();
        Map<Long, MenuVO> byId = new LinkedHashMap<>();
        for (SysResource menu : menus) {
            byId.put(menu.getId(), MenuVO.from(menu));
        }
        List<MenuVO> roots = new ArrayList<>();
        for (SysResource menu : menus) {
            MenuVO node = byId.get(menu.getId());
            Long parentId = menu.getParentId();
            MenuVO parent = parentId == null || parentId == 0L ? null : byId.get(parentId);
            if (parent == null) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        return roots;
    }

    public SysResource create(MenuRequest request) {
        SysResource entity = request.toEntity(null);
        validateBasics(entity);
        validateParent(entity.getParentId(), null);
        ensurePermsUnique(entity.getCode(), null);
        return resourceDao.save(entity);
    }

    public SysResource update(Long id, MenuRequest request) {
        resourceDao.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("菜单不存在"));
        SysResource entity = request.toEntity(id);
        validateBasics(entity);
        validateParent(entity.getParentId(), id);
        ensurePermsUnique(entity.getCode(), id);
        return resourceDao.save(entity);
    }

    public void delete(Long id) {
        resourceDao.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("菜单不存在"));
        if (resourceDao.countChildren(id) > 0) {
            throw new IllegalArgumentException("该菜单存在子节点，无法删除");
        }
        if (resourceDao.countRoleRefs(id) > 0) {
            throw new IllegalArgumentException("该菜单已被角色引用，无法删除");
        }
        resourceDao.deleteById(id);
    }

    private void validateBasics(SysResource entity) {
        String type = entity.getType();
        if (type == null || !TYPES.contains(type)) {
            throw new IllegalArgumentException("type 必须为 catalog、menu 或 button");
        }
        if (entity.getName() == null || entity.getName().isBlank()) {
            throw new IllegalArgumentException("名称不能为空");
        }
        if ("menu".equals(type) && (entity.getPath() == null || entity.getPath().isBlank())) {
            throw new IllegalArgumentException("menu 类型必须填写 path");
        }
        if ("button".equals(type) && (entity.getCode() == null || entity.getCode().isBlank())) {
            throw new IllegalArgumentException("button 类型必须填写 perms");
        }
        if ("button".equals(type)) {
            entity.setPath(null);
        }
        if ("catalog".equals(type)) {
            entity.setCode(null);
        }
    }

    private void validateParent(Long parentId, Long selfId) {
        if (parentId == null || parentId == 0L) {
            return;
        }
        if (parentId.equals(selfId)) {
            throw new IllegalArgumentException("父节点不能是自身");
        }
        Set<Long> seen = new HashSet<>();
        Long current = parentId;
        while (current != null && current != 0L) {
            if (current.equals(selfId)) {
                throw new IllegalArgumentException("父节点选择会形成环");
            }
            if (!seen.add(current)) {
                throw new IllegalArgumentException("父节点选择会形成环");
            }
            SysResource node = resourceDao.findById(current).orElse(null);
            if (node == null) {
                throw new IllegalArgumentException("父节点不存在");
            }
            current = node.getParentId();
        }
    }

    private void ensurePermsUnique(String perms, Long excludeId) {
        if (perms == null || perms.isBlank()) {
            return;
        }
        if (resourceDao.countByPerms(perms, excludeId) > 0) {
            throw new IllegalArgumentException("权限码已存在：" + perms);
        }
    }
}
