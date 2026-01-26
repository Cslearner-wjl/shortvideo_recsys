#!/usr/bin/env bash
# 用途: M4 ALS 验收脚本
# 用法: bash scripts/acceptance_m4_als.sh

set -euo pipefail

API_BASE="${API_BASE:-http://localhost:18080}"
VIDEO_ID="${VIDEO_ID:-1}"
USER_TOKEN="${USER_TOKEN:-}"
USER_ID="${USER_ID:-}"
ACCOUNT="${ACCOUNT:-}"
PASSWORD="${PASSWORD:-}"

REDIS_HOST="${REDIS_HOST:-127.0.0.1}"
REDIS_PORT="${REDIS_PORT:-6379}"

JDBC_URL="${JDBC_URL:-jdbc:mysql://127.0.0.1:3307/shortvideo_recsys?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai}"
JDBC_USER="${JDBC_USER:-app}"
JDBC_PASSWORD="${JDBC_PASSWORD:-apppass}"

if [[ -z "${USER_TOKEN}" || -z "${USER_ID}" ]]; then
  if [[ -n "${ACCOUNT}" && -n "${PASSWORD}" ]]; then
    LOGIN_RESP="$(curl -s -X POST "${API_BASE}/api/auth/login" -H "Content-Type: application/json" -d "{\"account\":\"${ACCOUNT}\",\"password\":\"${PASSWORD}\"}")"
    if command -v python3 >/dev/null 2>&1; then
      USER_TOKEN="$(python3 - <<PY
import json
data = json.loads(r'''$LOGIN_RESP''')
token = ((data.get("data") or {}) or {}).get("token")
print(token or "")
PY
)"
      USER_ID="$(python3 - <<PY
import json
data = json.loads(r'''$LOGIN_RESP''')
uid = (((data.get("data") or {}) or {}).get("user") or {}).get("id")
print(uid or "")
PY
)"
    else
      echo "缺少 python3，无法从登录响应解析 token/userId，请手动设置 USER_TOKEN 与 USER_ID。"
      exit 1
    fi
  else
    echo "请设置 USER_TOKEN/USER_ID 或提供 ACCOUNT/PASSWORD 用于登录。"
    exit 1
  fi
fi

echo "[1/5] 触发点赞事件: userId=${USER_ID}, videoId=${VIDEO_ID}"
curl -s -X POST "${API_BASE}/api/videos/${VIDEO_ID}/like" \
  -H "Authorization: Bearer ${USER_TOKEN}" >/dev/null

echo "[2/5] 运行 ALS 离线作业（JDBC -> Redis）"
export ALS_SOURCE=jdbc
export ALS_INPUT_TYPE=user_actions
export JDBC_URL JDBC_USER JDBC_PASSWORD
export REDIS_HOST REDIS_PORT
scripts/run_m4_als_batch.sh >/dev/null 2>&1

echo "[3/5] 验证 Redis 推荐列表（rec:user:${USER_ID}）"
if command -v redis-cli >/dev/null 2>&1; then
  redis-cli -h "${REDIS_HOST}" -p "${REDIS_PORT}" LRANGE "rec:user:${USER_ID}" 0 19 || true
elif docker ps --format '{{.Names}}' 2>/dev/null | grep -q '^shortvideo-recsys-redis$'; then
  docker exec -i shortvideo-recsys-redis redis-cli LRANGE "rec:user:${USER_ID}" 0 19 || true
else
  echo "未检测到 redis-cli 或 Redis 容器，跳过 Redis 校验。"
fi

echo
echo "[4/5] 请求推荐接口（需后端开启 RECO_ALS_ENABLED=true 才会优先读取 ALS）"
RESP="$(curl -s "${API_BASE}/api/recommendations?page=1&pageSize=20" -H "Authorization: Bearer ${USER_TOKEN}")"
if command -v python3 >/dev/null 2>&1; then
  echo "${RESP}" | python3 -m json.tool
else
  echo "${RESP}"
fi

echo
echo "[5/5] 验收提示"
echo "- 若后端已启用 ALS 开关（RECO_ALS_ENABLED=true），接口返回的前 N 个 videoId 应与 Redis 列表顺序一致。"
echo "- 若未启用或未命中，则会回退到规则推荐（仍可正常返回）。"
