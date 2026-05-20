package com.aios.platform.system.service;

import com.aios.platform.system.PermissionCatalog;
import com.aios.platform.system.SystemConstants;
import com.aios.platform.system.entity.SysMenu;
import com.aios.platform.system.entity.SysPermission;
import com.aios.platform.system.entity.SysRole;
import com.aios.platform.system.entity.SysRolePermission;
import com.aios.platform.system.mapper.SysMenuMapper;
import com.aios.platform.system.mapper.SysPermissionMapper;
import com.aios.platform.system.mapper.SysRoleMapper;
import com.aios.platform.system.mapper.SysRolePermissionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RbacSyncService {

    private final SysPermissionMapper permissionMapper;
    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysMenuMapper menuMapper;

    @Transactional
    public void syncPermissionsToCatalog() {
        Map<String, Long> codeToId = new HashMap<>();
        for (PermissionCatalog.Entry entry : PermissionCatalog.ALL) {
            Long permId = upsertPermission(entry.code(), entry.name());
            codeToId.put(entry.code(), permId);
        }
        grantAllToSuperAdmin(codeToId);
    }

    private Long upsertPermission(String code, String name) {
        SysPermission existing = permissionMapper.selectByCodeIncludeDeleted(code);
        if (existing != null) {
            boolean revived = existing.getDeleted() != null && existing.getDeleted() != 0;
            existing.setPermissionName(name);
            existing.setPermissionType("api");
            existing.setDeleted(0);
            permissionMapper.updateById(existing);
            if (revived) {
                log.info("RBAC: 恢复权限 {}", code);
            }
            return existing.getPermissionId();
        }
        SysPermission p = new SysPermission();
        p.setPermissionCode(code);
        p.setPermissionName(name);
        p.setPermissionType("api");
        try {
            permissionMapper.insert(p);
            log.info("RBAC: 新增权限 {}", code);
            return p.getPermissionId();
        } catch (DuplicateKeyException ex) {
            SysPermission row = permissionMapper.selectByCodeIncludeDeleted(code);
            if (row == null) {
                throw ex;
            }
            row.setPermissionName(name);
            row.setPermissionType("api");
            row.setDeleted(0);
            permissionMapper.updateById(row);
            log.info("RBAC: 权限已存在，已同步 {}", code);
            return row.getPermissionId();
        }
    }

    private void grantAllToSuperAdmin(Map<String, Long> codeToId) {
        SysRole superRole =
                roleMapper.selectOne(
                        new LambdaQueryWrapper<SysRole>()
                                .eq(SysRole::getRoleCode, SystemConstants.ROLE_SUPER_ADMIN));
        if (superRole == null) {
            return;
        }
        Long roleId = superRole.getRoleId();
        for (Long permId : codeToId.values()) {
            Long count =
                    rolePermissionMapper.selectCount(
                            new LambdaQueryWrapper<SysRolePermission>()
                                    .eq(SysRolePermission::getRoleId, roleId)
                                    .eq(SysRolePermission::getPermissionId, permId));
            if (count == null || count == 0) {
                SysRolePermission rp = new SysRolePermission();
                rp.setRoleId(roleId);
                rp.setPermissionId(permId);
                rolePermissionMapper.insert(rp);
            }
        }
    }

    public List<String> allPermissionCodes() {
        return permissionMapper.selectAllCodes();
    }

    @Transactional
    public void syncSystemMenus() {
        SysMenu sys =
                menuMapper.selectOne(
                        new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getPath, "/system").last("LIMIT 1"));
        if (sys == null) {
            return;
        }
        ensureChild(sys.getMenuId(), "角色管理", "/system/roles", "role:view", 2);
        ensureChild(sys.getMenuId(), "菜单管理", "/system/menus", "menu:view", 3);
        ensureChild(sys.getMenuId(), "权限管理", "/system/permissions", "permission:view", 4);
        patchChildPerm("/ai/models", "ai:model:view");
        patchChildPerm("/ai/agents", "ai:agent:view");
        ensureAiChild("MCP管理", "/ai/mcp-servers", "ai:mcp:view", 3);
        ensureAiChild("Skill管理", "/ai/skills", "ai:skill:view", 4);
        ensureAiChild("工作流管理", "/ai/workflows", "ai:workflow:view", 5);
        patchChildPerm("/kb/bases", "kb:base:view");
        patchChildPerm("/kb/documents", "kb:document:view");
        patchChildPerm("/chat/sessions", "chat:session:view");
        ensureChatChild("聊天中心", "/chat/sessions", "chat:session:view", 1);
        ensureInvokeChild("API调用中心", "/invoke", "invoke:workflow:view", 45);
        ensureInvokeChild("工作流调用", "/invoke/center", "invoke:workflow:view", 1);
        patchChildPerm("/dashboard", "dashboard:view");
    }

    private void ensureChild(Long parentId, String name, String path, String perm, int sort) {
        SysMenu m =
                menuMapper.selectOne(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getPath, path).last("LIMIT 1"));
        if (m == null) {
            m = new SysMenu();
            m.setParentId(parentId);
            m.setMenuName(name);
            m.setPath(path);
            m.setComponent(path);
            m.setSortOrder(sort);
            m.setPermissionCode(perm);
            m.setVisible(1);
            m.setStatus(1);
            menuMapper.insert(m);
            log.info("RBAC: 新增菜单 {}", path);
        } else {
            m.setPermissionCode(perm);
            m.setMenuName(name);
            m.setStatus(1);
            m.setVisible(1);
            menuMapper.updateById(m);
        }
    }

    private void ensureChatChild(String name, String path, String perm, int sort) {
        SysMenu chat =
                menuMapper.selectOne(
                        new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getPath, "/chat").last("LIMIT 1"));
        if (chat == null) {
            return;
        }
        ensureChild(chat.getMenuId(), name, path, perm, sort);
    }

    private void ensureAiChild(String name, String path, String perm, int sort) {
        SysMenu ai =
                menuMapper.selectOne(
                        new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getPath, "/ai").last("LIMIT 1"));
        if (ai == null) {
            return;
        }
        ensureChild(ai.getMenuId(), name, path, perm, sort);
    }

    private void ensureInvokeChild(String name, String path, String perm, int sort) {
        SysMenu parent =
                menuMapper.selectOne(
                        new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getPath, "/invoke").last("LIMIT 1"));
        if (parent == null) {
            SysMenu root = new SysMenu();
            root.setParentId(0L);
            root.setMenuName("API调用中心");
            root.setPath("/invoke");
            root.setComponent("Layout");
            root.setIcon("ApiOutlined");
            root.setSortOrder(45);
            root.setPermissionCode("invoke:workflow:view");
            root.setVisible(1);
            root.setStatus(1);
            menuMapper.insert(root);
            parent = root;
        }
        ensureChild(parent.getMenuId(), name, path, perm, sort);
    }

    private void patchChildPerm(String path, String perm) {
        SysMenu m =
                menuMapper.selectOne(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getPath, path).last("LIMIT 1"));
        if (m != null && (m.getPermissionCode() == null || m.getPermissionCode().isBlank())) {
            m.setPermissionCode(perm);
            menuMapper.updateById(m);
        }
    }
}
