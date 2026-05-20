# AIOS 平台 Wiki

面向产品、测试与运维的**图文操作手册**。各页面均附实际界面截图（`images/` 目录），与 [AIOS平台说明文档](../AIOS平台说明文档.md)、[系统架构介绍](../系统架构介绍.md) 互补。

---

## 架构文档

- **[系统架构介绍](../系统架构介绍.md)** — 逻辑架构、部署拓扑、模块划分、数据流与安全设计（含 Mermaid 图）

## 目录

| 章节 | 说明 |
|------|------|
| [登录与工作台](01-登录与工作台.md) | 登录、注册、仪表盘统计 |
| [AI 中心](02-AI中心.md) | 模型、Agent、MCP、Skill、工作流编排 |
| [知识库](03-知识库.md) | 知识库配置、文档录入与检索 |
| [聊天中心](04-聊天中心.md) | 工作流会话、气泡对话、Token 统计 |
| [API 调用中心](05-API调用中心.md) | 平台内 JWT / Open API 调试 |
| [系统管理](06-系统管理.md) | 用户、角色、菜单与权限 |

---

## 功能导航（路由）

| 菜单 | 路由 | 权限码（示例） |
|------|------|----------------|
| 工作台 | `/dashboard` | `dashboard:view` |
| 模型管理 | `/ai/models` | `ai:model:view` |
| Agent 管理 | `/ai/agents` | `ai:agent:view` |
| MCP 管理 | `/ai/mcp-servers` | `ai:mcp:view` |
| Skill 管理 | `/ai/skills` | `ai:skill:view` |
| 工作流管理 | `/ai/workflows` | `ai:workflow:view` |
| 知识库 | `/kb/bases` | `kb:base:view` |
| 文档管理 | `/kb/documents` | `kb:document:view` |
| 聊天中心 | `/chat/sessions` | `chat:session:view` |
| API 调用中心 | `/invoke/center` | `invoke:workflow:view` |
| 用户 / 角色 / 菜单 | `/system/*` | `user:view` 等 |

---

## 默认账号

首次启动数据库后自动初始化：

- **管理员**：`admin` / `admin`
- 业务库：`aios` @ `127.0.0.1:33061`（`aios` / `aios123`）

本地开发：前端 `http://127.0.0.1:5173`，后端 `http://127.0.0.1:8080`。

---

## 截图索引

| 文件 | 页面 |
|------|------|
| `01-login.png` | 登录 |
| `02-dashboard.png` | 工作台 |
| `03-ai-models.png` | 模型管理 |
| `04-ai-agents.png` | Agent 管理 |
| `05-ai-mcp.png` | MCP 管理 |
| `06-ai-skills.png` | Skill 管理 |
| `07-ai-workflows-list.png` | 工作流列表 |
| `07b-ai-workflows-editor.png` | 工作流编辑 |
| `08-kb-bases.png` | 知识库 |
| `09-kb-documents.png` | 文档管理 |
| `10-chat-sessions-list.png` | 聊天中心列表 |
| `10b-chat-drawer.png` | 会话抽屉 |
| `11-invoke-center.png` | API 调用中心 |
| `12-system-users.png` | 用户管理 |
| `13-system-roles.png` | 角色管理 |
| `14-system-menus.png` | 菜单管理 |
