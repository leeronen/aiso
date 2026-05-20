# AIOS 平台（PRD 骨架实现）

本仓库依据《AIOS 企业级 AI Agent 管理平台》PRD 搭建 **MVP 可运行骨架**：Spring Boot 3 + MyBatis Plus + JWT 后端，以及 Vite + React + TypeScript + Ant Design 管理端。

**完整功能说明、API 调用、工作流入参出参、Token 统计与安全建议见：** [docs/AIOS平台说明文档.md](docs/AIOS平台说明文档.md)

## 目录结构

| 路径 | 说明 |
|------|------|
| `sql/init.sql` | MySQL 8 业务库全量表结构 + 种子数据 |
| `sql/pgvector_init.sql` | PostgreSQL pgvector 向量表 |
| `sql/00_create_database.sql` | 仅建 MySQL 库 `aios` |
| `sql/migration_*.sql` | 业务库增量迁移 |
| `docker/docker-compose.yml` | MySQL + pgvector 双库 Compose |
| `scripts/db-init-all.sh` | 一键启动并初始化双库 |
| `scripts/db-init-mysql.sh` | 本机 MySQL 业务库初始化 |
| `scripts/db-init-pgvector.sh` | 本机 pgvector 初始化 |
| `scripts/db-migrate-mysql.sh` | 业务库增量迁移 |
| `backend/` | 单体后端 `aios-platform`（对应 PRD 第一阶段：登录/RBAC/模型/Agent/聊天/知识库 API） |
| `frontend/` | 管理端 Web（仪表盘、模型、Agent、知识库、文档、会话、用户） |

**已实现（相对早期骨架）：** 工作流画布与版本、DAG 运行时、聊天中心（工作流驱动多 Agent）、API 调用中心、Open API、Token 统计、知识库 RAG、全链路 TraceId。

未实现部分（按 PRD 后续阶段）：Spring Cloud Gateway、Nacos、完整 MCP 协议、流式输出、多租户等。详见说明文档。

## 环境要求

- JDK 17 + Maven 3.9+
- **MySQL 8**（业务库）+ **PostgreSQL 16 + pgvector**（向量库），或 Docker 一键启动
- Node 20+（前端）

## 数据库

### 方式一：Docker 一键启动（推荐）

同时启动 **MySQL 业务库** 与 **pgvector 向量库**，并在**首次启动**时自动执行初始化脚本：

```bash
./scripts/db-init-all.sh
# 或
docker compose -f docker/docker-compose.yml up -d
```

| 库 | 地址 | 库名 | 默认账号 |
|----|------|------|----------|
| MySQL 业务库 | `127.0.0.1:33061` | `aios` | `aios` / `aios123`（root / `root123`） |
| pgvector | `127.0.0.1:5432` | `aios_vector` | `aios` / `aios123` |

自动执行的 SQL：

- 业务库：`sql/init.sql`（表结构 + RBAC/菜单/Embedding 类型等种子数据）
- 向量库：`sql/pgvector_init.sql`（`vector` 扩展 + `kb_chunk_vector` 表）

> 注意：初始化脚本仅在**数据卷为空、容器第一次创建**时运行。若需重装，先执行  
> `docker compose -f docker/docker-compose.yml down -v`

仅启动向量库（已有 MySQL 时）：

```bash
./scripts/db-start-pgvector.sh
```

### 方式二：本机 MySQL / PostgreSQL 手动初始化

**业务库：**

```bash
./scripts/db-init-mysql.sh
# 或
mysql -u root -p < sql/00_create_database.sql
mysql -u root -p aios < sql/init.sql
```

**向量库：**

```bash
./scripts/db-init-pgvector.sh
```

本机未安装 `psql` 时会**自动使用 Docker** 启动 `aios-pgvector` 并导入 `sql/pgvector_init.sql`。也可一键双库：`./scripts/db-init-all.sh`。

若已安装客户端，可手动执行：`psql -U aios -d aios_vector -f sql/pgvector_init.sql`

**从旧版本升级（已有 `aios` 库）：**

```bash
./scripts/db-migrate-mysql.sh
```

增量脚本顺序见 `scripts/db-migrate-mysql.sh` 内列表（MCP/Skill、IO Schema、Agent 配置、文档字段、Embedding 类型、向量映射表等）。

### 后端配置对齐

`backend/src/main/resources/application.yml` 需与上述账号一致，例如 Docker 默认：

```yaml
spring.datasource:
  url: jdbc:mysql://127.0.0.1:33061/aios
  username: aios
  password: aios123

aios.vector.datasource:
  url: jdbc:postgresql://127.0.0.1:5432/aios_vector
  username: aios
  password: aios123
```

（与 `docker/docker-compose.yml` 及 `application.yml` 默认一致；root 账号为 `root` / `root123`，仅管理用途。）

按需修改 `aios.jwt.secret`、`aios.embedding.api-key`（向量化需要）。

### 启动失败排查

`mvn spring-boot:run` 若 exit code 1，请查看控制台 **Caused by** 一行，常见原因：

| 现象 | 处理 |
|------|------|
| `Communications link failure` / `Connection refused` | 启动 MySQL 容器，确认 `127.0.0.1:33061` 可连（见 docker-compose 端口映射） |
| `Unknown database 'aios'` | 先执行建库：`CREATE DATABASE aios ...` |
| `Table 'aios.sys_user' doesn't exist` | 执行 `mysql ... aios < sql/init.sql` |
| `Access denied for user 'root'@...` | 修改 `application.yml` 中 `username` / `password` |
| `required a bean of type '...Mapper' that could not be found` | 已修复：请拉取最新代码（`@MapperScan("com.aios.platform")`）后重新编译 |

## 后端

```bash
cd backend
mvn spring-boot:run
```

启动后 `DataInitializer` 会在 **不存在 `admin` 用户** 时自动创建：

- 用户：`admin` / `admin`
- 角色：`SUPER_ADMIN` 及 PRD 风格 API 权限
- 动态菜单树（与前端路由对齐）

主要 API 前缀：`/api`

### Swagger / OpenAPI

启动后端后访问：

- **Swagger UI**：http://127.0.0.1:8080/swagger-ui.html
- **OpenAPI JSON**：http://127.0.0.1:8080/v3/api-docs

调用需鉴权接口：先 `POST /api/auth/login` 获取 `accessToken`，在 Swagger 右上角 **Authorize** 填入 `Bearer <accessToken>`（或仅 token，视 UI 版本而定）。

## 前端

```bash
cd frontend
npm install
npm run dev
```

开发服务器默认 `http://127.0.0.1:5173`，通过 Vite 代理将 `/api` 转发到 `http://127.0.0.1:8080`。

## 说明

- 聊天与工作流调用已接入 **OpenAI 兼容 Chat API** 运行时（`WorkflowRuntimeService` / `LlmChatClient`），需配置模型 API Key。
- 模型 `API Key` 存储于数据库，生产环境建议脱敏返回并加密存储。
