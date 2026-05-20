package com.aios.platform.system;

import java.util.List;

/** 系统权限清单（模块:view/add/update/delete） */
public final class PermissionCatalog {

    public record Entry(String code, String name, String module) {}

    public static final List<Entry> ALL = List.of(
            e("dashboard:view", "仪表盘-查看", "dashboard"),
            e("ai:model:view", "模型-查看", "ai:model"),
            e("ai:model:add", "模型-新增", "ai:model"),
            e("ai:model:update", "模型-编辑", "ai:model"),
            e("ai:model:delete", "模型-删除", "ai:model"),
            e("ai:agent:view", "Agent-查看", "ai:agent"),
            e("ai:agent:add", "Agent-新增", "ai:agent"),
            e("ai:agent:update", "Agent-编辑", "ai:agent"),
            e("ai:agent:delete", "Agent-删除", "ai:agent"),
            e("ai:mcp:view", "MCP-查看", "ai:mcp"),
            e("ai:mcp:add", "MCP-新增", "ai:mcp"),
            e("ai:mcp:update", "MCP-编辑", "ai:mcp"),
            e("ai:mcp:delete", "MCP-删除", "ai:mcp"),
            e("ai:skill:view", "Skill-查看", "ai:skill"),
            e("ai:skill:add", "Skill-新增", "ai:skill"),
            e("ai:skill:update", "Skill-编辑", "ai:skill"),
            e("ai:skill:delete", "Skill-删除", "ai:skill"),
            e("ai:workflow:view", "工作流-查看", "ai:workflow"),
            e("ai:workflow:add", "工作流-新增", "ai:workflow"),
            e("ai:workflow:update", "工作流-编辑", "ai:workflow"),
            e("ai:workflow:delete", "工作流-删除", "ai:workflow"),
            e("kb:base:view", "知识库-查看", "kb:base"),
            e("kb:base:add", "知识库-新增", "kb:base"),
            e("kb:base:update", "知识库-编辑", "kb:base"),
            e("kb:base:delete", "知识库-删除", "kb:base"),
            e("kb:document:view", "文档-查看", "kb:document"),
            e("kb:document:add", "文档-新增", "kb:document"),
            e("kb:document:update", "文档-编辑", "kb:document"),
            e("kb:document:delete", "文档-删除", "kb:document"),
            e("chat:session:view", "会话-查看", "chat:session"),
            e("chat:session:add", "会话-新增", "chat:session"),
            e("chat:session:update", "会话-编辑", "chat:session"),
            e("chat:session:delete", "会话-删除", "chat:session"),
            e("invoke:workflow:view", "API调用-查看", "invoke:workflow"),
            e("invoke:workflow:run", "API调用-执行", "invoke:workflow"),
            e("user:view", "用户-查看", "user"),
            e("user:add", "用户-新增", "user"),
            e("user:update", "用户-编辑", "user"),
            e("user:delete", "用户-删除", "user"),
            e("role:view", "角色-查看", "role"),
            e("role:add", "角色-新增", "role"),
            e("role:update", "角色-编辑", "role"),
            e("role:delete", "角色-删除", "role"),
            e("role:assign", "角色-分配权限", "role"),
            e("menu:view", "菜单-查看", "menu"),
            e("menu:add", "菜单-新增", "menu"),
            e("menu:update", "菜单-编辑", "menu"),
            e("menu:delete", "菜单-删除", "menu"),
            e("permission:view", "权限-查看", "permission"),
            e("permission:add", "权限-新增", "permission"),
            e("permission:update", "权限-编辑", "permission"),
            e("permission:delete", "权限-删除", "permission"));

    private static Entry e(String code, String name, String module) {
        return new Entry(code, name, module);
    }

    private PermissionCatalog() {}
}
