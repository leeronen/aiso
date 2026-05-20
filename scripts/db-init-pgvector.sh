#!/usr/bin/env bash
# pgvector 向量库初始化
#
# 用法:
#   ./scripts/db-init-pgvector.sh              # 有 psql 则直连；否则自动走 Docker
#   USE_DOCKER=1 ./scripts/db-init-pgvector.sh # 强制使用 Docker
#
# 环境变量: PG_HOST PG_PORT PG_USER PG_PASSWORD PG_DATABASE

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
COMPOSE_PG="${ROOT}/docker/docker-compose.pgvector.yml"
INIT_SQL="${ROOT}/sql/pgvector_init.sql"

PG_HOST="${PG_HOST:-127.0.0.1}"
PG_PORT="${PG_PORT:-5432}"
PG_USER="${PG_USER:-aios}"
PG_PASSWORD="${PG_PASSWORD:-aios123}"
PG_DATABASE="${PG_DATABASE:-aios_vector}"
PG_CONTAINER="${PG_CONTAINER:-aios-pgvector}"

export PGPASSWORD="${PG_PASSWORD}"

has_psql() {
  command -v psql >/dev/null 2>&1
}

has_docker() {
  command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1
}

wait_pg_container() {
  local i=0
  while [[ $i -lt 60 ]]; do
    if docker exec "${PG_CONTAINER}" pg_isready -U "${PG_USER}" -d "${PG_DATABASE}" >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
    i=$((i + 1))
  done
  echo "错误: pgvector 容器未在预期时间内就绪" >&2
  return 1
}

pgvector_initialized_in_container() {
  docker exec "${PG_CONTAINER}" psql -U "${PG_USER}" -d "${PG_DATABASE}" -tc \
    "SELECT 1 FROM information_schema.tables WHERE table_schema='public' AND table_name='kb_chunk_vector'" \
    | grep -q 1
}

init_via_docker() {
  echo "==> 使用 Docker 启动并初始化 pgvector（本机无需安装 psql）..."
  export POSTGRES_PASSWORD="${PG_PASSWORD}"
  docker compose -f "${COMPOSE_PG}" up -d
  wait_pg_container

  if pgvector_initialized_in_container; then
    echo "==> 表已存在，跳过 sql/pgvector_init.sql（若需重装请先: docker compose -f docker/docker-compose.pgvector.yml down -v）"
  else
    echo "==> 导入 sql/pgvector_init.sql ..."
    docker exec -i "${PG_CONTAINER}" psql -U "${PG_USER}" -d "${PG_DATABASE}" < "${INIT_SQL}"
  fi

  echo "==> 完成。向量库: 127.0.0.1:${PG_PORT}/${PG_DATABASE} (用户 ${PG_USER})"
}

init_via_psql() {
  echo "==> 使用本机 psql 连接 ${PG_HOST}:${PG_PORT} ..."

  echo "==> 创建数据库 ${PG_DATABASE}（若不存在）..."
  psql -h "${PG_HOST}" -p "${PG_PORT}" -U "${PG_USER}" -d postgres -tc \
    "SELECT 1 FROM pg_database WHERE datname='${PG_DATABASE}'" | grep -q 1 \
    || psql -h "${PG_HOST}" -p "${PG_PORT}" -U "${PG_USER}" -d postgres -c "CREATE DATABASE ${PG_DATABASE};"

  echo "==> 导入 sql/pgvector_init.sql ..."
  psql -h "${PG_HOST}" -p "${PG_PORT}" -U "${PG_USER}" -d "${PG_DATABASE}" -f "${INIT_SQL}"

  echo "==> 完成。向量库: ${PG_HOST}:${PG_PORT}/${PG_DATABASE}"
}

main() {
  if [[ ! -f "${INIT_SQL}" ]]; then
    echo "错误: 找不到 ${INIT_SQL}" >&2
    exit 1
  fi

  if [[ "${USE_DOCKER:-0}" == "1" ]]; then
    if ! has_docker; then
      echo "错误: 已指定 USE_DOCKER=1 但未检测到可用的 Docker" >&2
      exit 1
    fi
    init_via_docker
    return
  fi

  if has_psql; then
    init_via_psql
    return
  fi

  if has_docker; then
    echo "提示: 本机未安装 psql，将自动改用 Docker 执行初始化。"
    init_via_docker
    return
  fi

  cat >&2 <<'EOF'
错误: 未找到 psql，且 Docker 不可用。

可选方案:
  1) 安装 PostgreSQL 客户端后重试
     macOS:  brew install libpq && brew link --force libpq
     Ubuntu: sudo apt install postgresql-client

  2) 安装并启动 Docker 后执行:
     ./scripts/db-init-pgvector.sh
     或一键双库: ./scripts/db-init-all.sh
EOF
  exit 1
}

main "$@"
