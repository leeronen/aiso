#!/usr/bin/env bash
# 一键启动 Docker 双库并完成首次初始化（MySQL 业务库 + pgvector）
#
# 用法:
#   ./scripts/db-init-all.sh
#   MYSQL_ROOT_PASSWORD=xxx ./scripts/db-init-all.sh
#
# 适用: 本机已安装 Docker / Docker Compose

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
COMPOSE_FILE="${ROOT}/docker/docker-compose.yml"

export MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-root123}"
export MYSQL_PASSWORD="${MYSQL_PASSWORD:-aios123}"
export PG_PASSWORD="${PG_PASSWORD:-aios123}"

echo "==> 启动 MySQL + pgvector (docker compose)..."
docker compose -f "${COMPOSE_FILE}" up -d

echo "==> 等待服务就绪..."
docker compose -f "${COMPOSE_FILE}" ps

echo ""
echo "=========================================="
echo " 数据库已启动并完成首次初始化"
echo "=========================================="
echo ""
echo "【MySQL 业务库】"
echo "  库名:     aios"
echo "  地址:     127.0.0.1:${MYSQL_PORT:-33061}"
echo "  用户:     aios / ${MYSQL_PASSWORD}"
echo "  root:     root / ${MYSQL_ROOT_PASSWORD}"
echo "  脚本:     sql/init.sql（容器首次启动已自动执行）"
echo ""
echo "【PostgreSQL 向量库 pgvector】"
echo "  库名:     aios_vector"
echo "  地址:     127.0.0.1:${PG_PORT:-5432}"
echo "  用户:     aios / ${PG_PASSWORD}"
echo "  脚本:     sql/pgvector_init.sql（容器首次启动已自动执行）"
echo ""
echo "请确认 backend/src/main/resources/application.yml 中账号密码与上一致。"
echo "然后启动后端: cd backend && mvn spring-boot:run"
echo ""
