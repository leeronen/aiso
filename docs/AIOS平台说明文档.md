# AIOS 企业级 AI Agent 管理平台 — 使用说明

本文档说明当前仓库已实现的功能、部署方式、核心业务流程、API 调用方式、数据库迁移与测试方法。适用于开发、测试与运维人员。

**图文 Wiki（各页面截图）：** [wiki/Home.md](wiki/Home.md)

---

## 1. 项目概述

AIOS 是一套企业级 AI Agent 管理平台，支持：

- **模型 / Agent / MCP / Skill** 配置与关联
- **工作流** 可视化编排（DAG 分层执行，保存自动版本）
- **知识库** 文档上传、向量化与 RAG 检索
- **聊天中心** 会话绑定工作流，多 Agent 链式对话
- **API 调用中心** 按工作流入参/出参 Schema 调试与 Open API 对外调用
- **RBAC** 用户、角色、菜单、权限

技术栈：

| 层级 | 技术 |
|------|------|
| 后端 | Java 17、Spring Boot 3.3、Spring Security、JWT、MyBatis Plus |
| 前端 | React 18、TypeScript、Vite、Ant Design、React Flow |
| 业务库 | MySQL 8 |
| 向量库 | PostgreSQL 16 + pgvector |
| 大模型 | OpenAI 兼容 Chat Completions / Embeddings HTTP API |

---

## 2. 目录结构

```
aios-project/
├── backend/                 # Spring Boot 单体后端
├── frontend/                # 管理端 Web
├── sql/                     # 建库与迁移脚本
├── docker/docker-compose.yml
├── scripts/                 # 数据库一键脚本
├── docs/                    # 说明文档（本文档）
└── README.md                # 快速启动摘要
```

---

## 3. 环境要求与快速启动

### 3.1 环境

- JDK 17+、Maven 3.9+
- Node.js 20+
- Docker（推荐，用于 MySQL + pgvector）

### 3.2 启动数据库

```bash
./scripts/db-init-all.sh
# 或
docker compose -f docker/docker-compose.yml up -d
```

| 服务 | 地址 | 库名 | 默认账号 |
|------|------|------|----------|
| MySQL | `127.0.0.1:33061` | `aios` | `aios` / `aios123` |
| pgvector | `127.0.0.1:5432` | `aios_vector` | `aios` / `aios123` |

首次启动会自动执行 `sql/init.sql` 与 `sql/pgvector_init.sql`。重装需先：`docker compose -f docker/docker-compose.yml down -v`。

### 3.3 已有库增量迁移

若从旧版本升级，除 `scripts/db-migrate-mysql.sh` 内脚本外，请按需手动执行：

```bash
mysql -h 127.0.0.1 -P 33061 -u aios -paios123 aios < sql/migration_workflow_version.sql
mysql -h 127.0.0.1 -P 33061 -u aios -paios123 aios < sql/migration_chat_workflow.sql
mysql -h 127.0.0.1 -P 33061 -u aios -paios123 aios < sql/migration_token_usage_extend.sql
# 演示 Open API Key（仅开发环境，生产勿用）
mysql -h 127.0.0.1 -P 33061 -u aios -paios123 aios < sql/migration_demo_api_key.sql
```

### 3.4 启动后端

```bash
cd backend
mvn spring-boot:run
```

默认端口：`8080`。首次启动若无 `admin` 用户，会自动创建：

- 用户名：`admin`
- 密码：`admin`（**生产环境务必修改**）

配置见 `backend/src/main/resources/application.yml`：

- `spring.datasource` — MySQL
- `aios.vector.datasource` — PostgreSQL
- `aios.embedding.api-key` / 环境变量 `OPENAI_API_KEY` — 向量化与大模型
- `aios.jwt.secret` — JWT 签名密钥（**生产必须更换**）

### 3.5 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问：`http://127.0.0.1:5173`，`/api` 由 Vite 代理到 `http://127.0.0.1:8080`。

### 3.6 Swagger

- UI：http://127.0.0.1:8080/swagger-ui.html
- 需先登录获取 `accessToken`，在 Authorize 中填写 `Bearer <token>`

---

## 4. 功能模块说明

### 4.1 工作台

- 路径：`/dashboard`
- 系统概览（仪表盘）

### 4.2 AI 中心

| 菜单 | 路径 | 说明 |
|------|------|------|
| 模型管理 | `/ai/models` | 配置大模型 `baseUrl`、`modelCode`、API Key、温度等 |
| Agent 管理 | `/ai/agents` | 绑定模型、系统提示词、记忆/工具模式、知识库/MCP/Skill |
| MCP 管理 | `/ai/mcp-servers` | MCP 服务元数据（运行时描述注入，非完整 MCP 协议） |
| Skill 管理 | `/ai/skills` | 技能描述，可关联子工作流或 MCP |
| 工作流管理 | `/ai/workflows` | 画布编排 Agent 节点；配置入参/出参类型与 JSON Schema；每次保存自动递增版本，支持历史恢复 |

**工作流执行模型：**

- 画布保存后生成 DSL：`nodes`、`executionLayers`（DAG 拓扑分层，同层可并行）
- 无「顺序/并行」执行模式开关，由图结构自动解析

### 4.3 知识库

| 菜单 | 路径 | 说明 |
|------|------|------|
| 知识库管理 | `/kb/bases` | 创建知识库、分块策略、Embedding 类型 |
| 文档管理 | `/kb/documents` | 上传文件或 URL 导入、索引、RAG 检索 |

向量化依赖 `aios.embedding` 与 pgvector；未配置 API Key 时可能使用零向量占位（仅调试）。

### 4.4 聊天中心

| 菜单 | 路径 | 说明 |
|------|------|------|
| 聊天中心 | `/chat/sessions` | 左侧选择/管理工作流；新建会话须绑定工作流；右侧会话列表 |

**会话对话：**

1. 左侧选择工作流（可在此新建/编辑/删除工作流）
2. 新建会话 → 打开抽屉
3. 发送消息 → 按工作流 DSL **分层执行**各 Agent 节点
4. 界面布局：**客人（user）消息在右侧**，**系统（assistant）回复在左侧**（气泡样式）
5. 展示 Token 用量与耗时；支持会话级 Token 统计

**运行时链路：**

```
用户消息 → ChatService → WorkflowRuntimeService
  → 按 executionLayers 执行 → AgentRuntimeService
  → 知识库 RAG / MCP·Skill 描述 / 记忆模式 → LlmChatClient
  → 写入 assistant 消息 & ai_token_usage 明细
```

### 4.5 API 调用中心

| 菜单 | 路径 | 说明 |
|------|------|------|
| 工作流调用 | `/invoke/center` | 选择工作流，按入参 Schema 填表或 JSON，执行调用 |

**能力：**

- **平台内调用**：`POST /api/invoke/workflows/{id}`（JWT，权限 `invoke:workflow:run`）
- **Open API 调用**：`POST /open/v1/workflows/{id}/invoke`（Header `X-API-Key`）
- 支持自定义 **HTTP Headers**（如 `X-API-Key`、`Authorization`）
- 返回结果按工作流 **出参类型** 组装（text / message / object / json / array）

---

## 5. 工作流入参 / 出参

在工作流编辑页配置：

| 字段 | 说明 |
|------|------|
| `inputType` / `outputType` | `text`、`json`、`object`、`array`、`tool`、`message` |
| `inputSchema` / `outputSchema` | JSON Schema 或示例 JSON，用于校验与表单生成 |

**入参解析规则（`WorkflowIoSupport`）：**

| 类型 | 行为 |
|------|------|
| text | 纯文本或 `{ "content": "..." }` |
| message | `{ "role", "content" }`，取 content 作为提示词 |
| object | 校验 `required` 字段；优先取 `query`/`content` 等字段，否则整段 JSON 作为提示词 |

**出参组装：**

| 类型 | 返回示例 |
|------|----------|
| text | `{ "content": "..." }` |
| message | `{ "role": "assistant", "content": "..." }` |
| object | `{ "answer": "...", "steps": [...], "totalTokens": n }` 等 |

---

## 6. API 参考摘要

### 6.1 认证

```http
POST /api/auth/login
Content-Type: application/json

{ "username": "admin", "password": "admin" }
```

响应 `data.accessToken` 用于后续 `Authorization: Bearer <token>`。

全链路支持请求头 **`X-Trace-Id`**（前端自动生成），响应 `ApiResponse.traceId` 与日志一致。

### 6.2 聊天

| 方法 | 路径 | 权限 |
|------|------|------|
| GET | `/api/chat/sessions` | `chat:session:view` |
| POST | `/api/chat/sessions` | `chat:session:add` |
| GET | `/api/chat/sessions/{id}/messages` | `chat:session:view` |
| POST | `/api/chat/sessions/{id}/messages` | `chat:session:add` |
| GET | `/api/chat/sessions/{id}/token-stats` | `chat:session:view` |

创建会话示例：

```json
{ "workflowId": 1, "title": "测试会话", "agentId": null }
```

### 6.3 工作流调用（平台内）

```http
GET /api/invoke/workflows/{workflowId}/io
Authorization: Bearer <token>

POST /api/invoke/workflows/{workflowId}
Authorization: Bearer <token>
Content-Type: application/json

{ "query": "你好" }
```

### 6.4 Open API（对外）

```http
GET /open/v1/workflows/{workflowId}/io
X-API-Key: <your-api-key>

POST /open/v1/workflows/{workflowId}/invoke
X-API-Key: <your-api-key>
Content-Type: application/json

{ "query": "你好" }
```

演示 Key（仅开发，见 `sql/migration_demo_api_key.sql`）：`aios-demo-key-change-me`

---

## 7. Token 统计

| 存储位置 | 说明 |
|----------|------|
| `chat_message.token_count` | 用户消息为估算值；助手消息为当次工作流 LLM 合计 |
| `ai_token_usage` | 每个 Agent 节点每次 LLM 调用一条明细（prompt/completion/total） |

会话统计接口：`GET /api/chat/sessions/{sessionId}/token-stats`

LLM 响应中的 `usage` 字段优先；缺失时按字符粗算（约每 3 字符 1 token）。

---

## 8. 权限说明

权限编码格式：`模块:资源:操作`，例如 `ai:workflow:view`。

与 API 调用相关：

| 权限 | 说明 |
|------|------|
| `invoke:workflow:view` | 查看 IO Schema、调用中心菜单 |
| `invoke:workflow:run` | 执行工作流调用 |

超级管理员角色 `SUPER_ADMIN` 在启动时由 `RbacSyncService` 同步全部权限。

菜单由 `sys_menu` 驱动，前端 `AppLayout` 按 `/api/system/menus/nav` 动态渲染。

---

## 9. 单元测试

后端测试位于 `backend/src/test/java`，使用 **JUnit 5** + **Mockito**。

```bash
cd backend
mvn test
```

| 测试类 | 覆盖内容 |
|--------|----------|
| `WorkflowDslParserTest` | DSL 解析、分层、异常 |
| `WorkflowIoSupportTest` | 入参校验、提示词提取、出参格式 |
| `IoSchemaSupportTest` | Schema 类型与 JSON 校验 |
| `TokenEstimatorTest` / `LlmTokenUsageTest` | Token 估算与汇总 |
| `OpenApiKeyServiceTest` | API Key 校验逻辑 |
| `AiTokenUsageServiceTest` | Token 用量落库 |
| `KbFileStorageServiceTest` | 文件路径穿越防护 |
| `EmbeddingChunkStrategyTest` | 文档分块策略 |

---

## 10. 安全与生产建议

以下为审计要点摘要，**生产部署前请务必处理**：

| 优先级 | 项 | 建议 |
|--------|-----|------|
| P0 | 默认账号/密钥 | 修改 `admin` 密码、JWT secret、数据库密码；勿使用演示 API Key |
| P0 | 模型 API Key 泄露 | 列表/详情接口勿返回完整 `apiKey`，应脱敏 |
| P1 | SSRF | 限制 `AiModel.baseUrl`、知识库 URL 导入的目标地址（禁止内网 IP） |
| P1 | Open API Key | 哈希存储；按工作流授权；限流 |
| P1 | 权限提升 | `user:update` 不应任意授予 `admin` 超级管理员 |
| P2 | 前端 Token | 避免长期 refresh token 存 `localStorage`；生产用 HttpOnly Cookie 或短效 token |
| P2 | Swagger | 生产环境关闭或加鉴权 |

聊天会话已实现 **归属校验**（`ChatService.assertSessionOwner`），防止跨用户访问会话。

---

## 11. 常见问题

**Q：发送聊天无回复或报 API Key 错误？**  
A：在模型管理中配置有效 API Key，或设置环境变量 `OPENAI_API_KEY` / `aios.embedding.api-key`。

**Q：工作流执行报 DSL 为空？**  
A：在工作流管理页打开画布并保存，确保存在 Agent 节点与 `executionLayers`。

**Q：RAG 检索无结果？**  
A：确认 pgvector 已启动、文档已索引成功、Embedding API 可用。

**Q：Open API 返回 401？**  
A：检查 Header `X-API-Key` 是否正确，Key 是否在 `api_key` 表中且 `status=1`。

**Q：`Unknown column 'input_type'`？**  
A：对已有库执行 `sql/migration_workflow.sql` 等工作流相关迁移。

**Q：页面白屏？**  
A：查看浏览器控制台；常见为前端编译错误或接口 401，需重新登录。

---

## 12. 后续规划（未实现）

- Spring Cloud 微服务拆分、Gateway、Nacos
- 完整 MCP JSON-RPC 工具调用
- 流式输出（SSE）
- API Key 管理界面、调用审计
- 多租户与知识库数据隔离
- 生产级密钥管理与 SSRF 防护组件

---

## 13. 相关文档

- [README.md](../README.md) — 快速启动与数据库说明
- Swagger — 运行时完整 API 列表

如有问题，请结合响应中的 `traceId` 在后端日志中检索全链路记录。
