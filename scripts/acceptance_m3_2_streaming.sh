#!/usr/bin/env bash
# 用途: M3.2 流式链路验收脚本
# 用法: bash scripts/acceptance_m3_2_streaming.sh

set -euo pipefail

API_BASE="${API_BASE:-http://localhost:18080}"
VIDEO_ID="${VIDEO_ID:-1}"
USER_TOKEN="${USER_TOKEN:-}"
ACCOUNT="${ACCOUNT:-}"
PASSWORD="${PASSWORD:-}"
REDIS_HOST="${REDIS_HOST:-127.0.0.1}"
REDIS_PORT="${REDIS_PORT:-6379}"

if [[ -z "${USER_TOKEN}" ]]; then
  if [[ -n "${ACCOUNT}" && -n "${PASSWORD}" ]]; then
    LOGIN_RESP="$(curl -s -X POST "${API_BASE}/api/auth/login" -H "Content-Type: application/json" -d "{\"account\":\"${ACCOUNT}\",\"password\":\"${PASSWORD}\"}")"
    if command -v python3 >/dev/null 2>&1; then
      USER_TOKEN="$(python3 - <<PY
import json
data = json.loads(r'''$LOGIN_RESP''')
token = (data.get("data") or {}).get("token") or data.get("token")
print(token or "")
PY
)"
    else
      echo "缺少 python3，无法从登录响应解析 token，请手动设置 USER_TOKEN。"
      exit 1
    fi
  else
    echo "请设置 USER_TOKEN 或提供 ACCOUNT/PASSWORD 用于登录。"
    exit 1
  fi
fi

echo "[1/3] 触发点赞事件: videoId=${VIDEO_ID}"
curl -s -X POST "${API_BASE}/api/videos/${VIDEO_ID}/like" \
  -H "Authorization: Bearer ${USER_TOKEN}" >/dev/null

echo "[2/3] 等待 Streaming 写入 Redis..."
sleep 3

if command -v redis-cli >/dev/null 2>&1; then
  echo "Redis Hash: stats:video:${VIDEO_ID}"
  redis-cli -h "${REDIS_HOST}" -p "${REDIS_PORT}" HGETALL "stats:video:${VIDEO_ID}"
  echo
  echo "Redis ZSET: hot:videos (score for videoId)"
  redis-cli -h "${REDIS_HOST}" -p "${REDIS_PORT}" ZSCORE "hot:videos" "${VIDEO_ID}" || true
elif docker ps --format '{{.Names}}' 2>/dev/null | grep -q '^shortvideo-recsys-redis$'; then
  echo "Redis Hash(容器内): stats:video:${VIDEO_ID}"
  docker exec -i shortvideo-recsys-redis redis-cli HGETALL "stats:video:${VIDEO_ID}" || true
  echo
  echo "Redis ZSET(容器内): hot:videos (score for videoId)"
  docker exec -i shortvideo-recsys-redis redis-cli ZSCORE "hot:videos" "${VIDEO_ID}" || true
else
  echo "未检测到 redis-cli，跳过 Redis 校验。"
fi

echo
echo "[3/3] 请求热门榜接口（应优先返回 Redis 统计）"
curl -s "${API_BASE}/api/rank/hot?page=1&pageSize=20" | (command -v python3 >/dev/null 2>&1 && python3 -m json.tool || cat)
