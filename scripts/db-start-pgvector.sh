#!/usr/bin/env bash
# 仅启动 pgvector（已有 MySQL 时使用）
#
# 用法: ./scripts/db-start-pgvector.sh

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
export PG_PASSWORD="${PG_PASSWORD:-aios123}"

docker compose -f "${ROOT}/docker/docker-compose.pgvector.yml" up -d

echo "pgvector 已启动: 127.0.0.1:5432/aios_vector (aios / ${PG_PASSWORD})"
