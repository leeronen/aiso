#!/usr/bin/env bash
# 业务库增量迁移（已有 aios 库、从旧版本升级时执行）
#
# 用法:
#   ./scripts/db-migrate-mysql.sh

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

MIGRATIONS=(
  migration_mcp_skill_models.sql
  migration_io_schema.sql
  migration_agent_config.sql
  migration_kb_document.sql
  migration_kb_embedding_type.sql
  migration_kb_vector_ref.sql
  migration_workflow.sql
)

for f in "${MIGRATIONS[@]}"; do
  path="${ROOT}/sql/${f}"
  if [[ -f "${path}" ]]; then
    echo "==> 执行 ${f} ..."
    mysql_cmd "${MYSQL_DATABASE}" < "${path}" || echo "    (部分语句可能已执行过，可忽略重复列错误)"
  fi
done

echo "==> MySQL 迁移脚本执行完毕。"
