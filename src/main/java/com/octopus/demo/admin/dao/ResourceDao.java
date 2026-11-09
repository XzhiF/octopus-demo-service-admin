package com.octopus.demo.admin.dao;

import com.octopus.demo.admin.entity.SysResource;
import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
import java.util.Optional;

public interface ResourceDao {
    SysResource save(SysResource resource);
    Optional<SysResource> findById(Long id);
    Optional<SysResource> findByCode(String code);
    java.util.List<SysResource> findAll();
    void deleteById(Long id);
    PageResultBean<SysResource> findAll(PageQueryBean query);

    /** Direct children of the node, used to block deleting a menu that still has a subtree. */
    int countChildren(Long id);

    /** sys_role_menu references, used to block deleting a granted menu. */
    int countRoleRefs(Long id);

    /** Global perms uniqueness check; {@code excludeId} skips the row being updated. */
    int countByPerms(String perms, Long excludeId);

    /** Visible catalog/menu nodes granted to the user, ordered by sort. */
    java.util.List<SysResource> findVisibleByUserId(Long userId);
}
