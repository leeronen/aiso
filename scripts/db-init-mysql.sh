#!/usr/bin/env bash
# 业务库 MySQL 初始化（不使用 Docker 时，连接本机 MySQL）
#
# 用法:
#   ./scripts/db-init-mysql.sh
#   MYSQL_HOST=127.0.0.1 MYSQL_USER=root MYSQL_PASSWORD=xxx ./scripts/db-init-mysql.sh

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_USER="${MYSQL_USER:-aios}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-aios123}"
MYSQL_DATABASE="${MYSQL_DATABASE:-aios}"

mysql_cmd() {
  if [[ -n "${MYSQL_PASSWORD}" ]]; then
    mysql -h "${MYSQL_HOST}" -P "${MYSQL_PORT}" -u "${MYSQL_USER}" -p"${MYSQL_PASSWORD}" "$@"
  else
    mysql -h "${MYSQL_HOST}" -P "${MYSQL_PORT}" -u "${MYSQL_USER}" "$@"
  fi
}

echo "==> 创建数据库 ${MYSQL_DATABASE} ..."
mysql_cmd < "${ROOT}/sql/00_create_database.sql"

echo "==> 导入业务表结构及种子数据 sql/init.sql ..."
mysql_cmd "${MYSQL_DATABASE}" < "${ROOT}/sql/init.sql"

echo "==> 完成。业务库: ${MYSQL_HOST}:${MYSQL_PORT}/${MYSQL_DATABASE}"
