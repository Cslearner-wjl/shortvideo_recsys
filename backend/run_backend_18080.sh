#!/usr/bin/env bash
# 用途: 启动后端并监听 18080 端口
# 用法: bash backend/run_backend_18080.sh

set -euo pipefail

cd "$(dirname "$0")"

REPO_ROOT="$(cd "$(pwd)/.." && pwd)"

export SERVER_PORT=18080
export SPRING_PROFILES_ACTIVE=docker
export MYSQL_HOST=127.0.0.1
export MYSQL_PORT=3307
export MYSQL_DATABASE=shortvideo_recsys
export MYSQL_USER=app
export MYSQL_PASSWORD=apppass
export REDIS_HOST=127.0.0.1
export REDIS_PORT=6379

# Behavior event log (for Flume taildir -> Kafka behavior-events)
# Spring binds `app.behavior-log.path` from env var `APP_BEHAVIOR_LOG_PATH`.
# For compatibility with docs/older commands, if `BEHAVIOR_LOG_PATH` is set, map it through.
if [[ -n "${BEHAVIOR_LOG_PATH:-}" ]]; then
	export APP_BEHAVIOR_LOG_PATH="${BEHAVIOR_LOG_PATH}"
else
	export APP_BEHAVIOR_LOG_PATH="${REPO_ROOT}/deploy/data/behavior/behavior-events.log"
fi

# 开发联调用：确保管理端 Basic Auth 有可用账号（admin/AdminPass123）。
export APP_ADMIN_BOOTSTRAP_RESET_PASSWORD=true

exec mvn -DskipTests spring-boot:run
