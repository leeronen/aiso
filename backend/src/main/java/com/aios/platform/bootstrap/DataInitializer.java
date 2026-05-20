package com.aios.platform.bootstrap;

import com.aios.platform.system.SystemConstants;
import com.aios.platform.system.entity.SysMenu;
import com.aios.platform.system.entity.SysRole;
import com.aios.platform.system.entity.SysUser;
import com.aios.platform.system.entity.SysUserRole;
import com.aios.platform.system.mapper.SysMenuMapper;
import com.aios.platform.system.mapper.SysRoleMapper;
import com.aios.platform.system.mapper.SysUserMapper;
import com.aios.platform.system.mapper.SysUserRoleMapper;
import com.aios.platform.system.service.RbacSyncService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Order(2)
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysMenuMapper menuMapper;
    private final RbacSyncService rbacSyncService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        try {
            initIfNeeded();
        } catch (DataAccessException ex) {
            log.error(
                    "数据库初始化失败。请确认：1) MySQL 已启动；2) 已创建库 aios；3) 已执行 sql/init.sql。错误: {}",
                    ex.getMessage());
            throw ex;
        }
    }

    private void initIfNeeded() {
        Long exists =
                userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, "admin"));
        if (exists != null && exists > 0) {
            return;
        }

        SysRole role =
                roleMapper.selectOne(
                        new LambdaQueryWrapper<SysRole>()
                                .eq(SysRole::getRoleCode, SystemConstants.ROLE_SUPER_ADMIN));
        if (role == null) {
            role = new SysRole();
            role.setRoleName("超级管理员");
            role.setRoleCode(SystemConstants.ROLE_SUPER_ADMIN);
            role.setDescription("内置角色，拥有全部 MVP 权限");
            role.setStatus(1);
            roleMapper.insert(role);
        }
        Long roleId = role.getRoleId();

        rbacSyncService.syncPermissionsToCatalog();

        SysUser admin = new SysUser();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin"));
        admin.setNickname("Administrator");
        admin.setEmail("admin@aios.local");
        admin.setStatus(1);
        userMapper.insert(admin);

        SysUserRole ur = new SysUserRole();
        ur.setUserId(admin.getUserId());
        ur.setRoleId(roleId);
        userRoleMapper.insert(ur);

        seedMenus();
    }

    private void seedMenus() {
        SysMenu workbench = menu("工作台", 0L, "/workbench", "Layout", "HomeOutlined", 10, "dashboard:view");
        menuMapper.insert(workbench);
        insertChild(workbench.getMenuId(), "Dashboard", "/dashboard", "dashboard:view", 1);

        SysMenu ai = menu("AI中心", 0L, "/ai", "Layout", "RobotOutlined", 20, "ai:model:view");
        menuMapper.insert(ai);
        insertChild(ai.getMenuId(), "模型管理", "/ai/models", "ai:model:view", 1);
        insertChild(ai.getMenuId(), "Agent管理", "/ai/agents", "ai:agent:view", 2);
        insertChild(ai.getMenuId(), "MCP管理", "/ai/mcp-servers", "ai:mcp:view", 3);
        insertChild(ai.getMenuId(), "Skill管理", "/ai/skills", "ai:skill:view", 4);
        insertChild(ai.getMenuId(), "工作流管理", "/ai/workflows", "ai:workflow:view", 5);

        SysMenu kb = menu("知识库", 0L, "/kb", "Layout", "BookOutlined", 30, "kb:base:view");
        menuMapper.insert(kb);
        insertChild(kb.getMenuId(), "知识库管理", "/kb/bases", "kb:base:view", 1);
        insertChild(kb.getMenuId(), "文档管理", "/kb/documents", "kb:document:view", 2);

        SysMenu chat = menu("聊天中心", 0L, "/chat", "Layout", "CommentOutlined", 40, "chat:session:view");
        menuMapper.insert(chat);
        insertChild(chat.getMenuId(), "聊天中心", "/chat/sessions", "chat:session:view", 1);

        SysMenu invoke = menu("API调用中心", 0L, "/invoke", "Layout", "ApiOutlined", 45, "invoke:workflow:view");
        menuMapper.insert(invoke);
        insertChild(invoke.getMenuId(), "工作流调用", "/invoke/center", "invoke:workflow:view", 1);

        SysMenu sys = menu("系统管理", 0L, "/system", "Layout", "SettingOutlined", 90, "user:view");
        menuMapper.insert(sys);
        insertChild(sys.getMenuId(), "用户管理", "/system/users", "user:view", 1);
        insertChild(sys.getMenuId(), "角色管理", "/system/roles", "role:view", 2);
        insertChild(sys.getMenuId(), "菜单管理", "/system/menus", "menu:view", 3);
        insertChild(sys.getMenuId(), "权限管理", "/system/permissions", "permission:view", 4);
    }

    private SysMenu menu(String name, Long parent, String path, String comp, String icon, int sort, String perm) {
        SysMenu m = new SysMenu();
        m.setParentId(parent);
        m.setMenuName(name);
        m.setPath(path);
        m.setComponent(comp);
        m.setIcon(icon);
        m.setSortOrder(sort);
        m.setPermissionCode(perm);
        m.setVisible(1);
        m.setStatus(1);
        return m;
    }

    private void insertChild(Long parentId, String name, String path, String permissionCode, int sort) {
        SysMenu m = new SysMenu();
        m.setParentId(parentId);
        m.setMenuName(name);
        m.setPath(path);
        m.setComponent(path);
        m.setPermissionCode(permissionCode);
        m.setSortOrder(sort);
        m.setVisible(1);
        m.setStatus(1);
        menuMapper.insert(m);
    }
}
