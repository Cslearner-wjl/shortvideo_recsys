#!/usr/bin/env bash
# 用途: 验证行为事件写入与消费
# 用法: bash scripts/verify_behavior_events.sh

set -euo pipefail

API_BASE="${API_BASE:-http://localhost:18080}"
VIDEO_ID="${VIDEO_ID:-}"
USER_TOKEN="${USER_TOKEN:-}"
ACCOUNT="${ACCOUNT:-}"
PASSWORD="${PASSWORD:-}"

# Where backend should append JSONL behavior events.
# Note: backend must be started with BEHAVIOR_LOG_PATH pointing to this file for Flume to pick up.
BEHAVIOR_LOG_FILE="${BEHAVIOR_LOG_FILE:-deploy/data/behavior/behavior-events.log}"

# Kafka verification settings
KAFKA_TOPIC="${KAFKA_TOPIC:-behavior-events}"
KAFKA_TIMEOUT_MS="${KAFKA_TIMEOUT_MS:-30000}"
# When running on host, docker-compose exposes Kafka on 9093 by default.
KAFKA_BOOTSTRAP="${KAFKA_BOOTSTRAP:-localhost:${KAFKA_EXTERNAL_PORT:-9093}}"

detect_kafka_container() {
  if [[ -n "${KAFKA_CONTAINER:-}" ]] && docker inspect "${KAFKA_CONTAINER}" >/dev/null 2>&1; then
    echo "${KAFKA_CONTAINER}"
    return 0
  fi

  for name in kafka shortvideo-recsys-kafka; do
    if docker inspect "${name}" >/dev/null 2>&1; then
      echo "${name}"
      return 0
    fi
  done

  return 1
}

file_size_bytes() {
  local path="$1"
  if [[ -f "${path}" ]]; then
    wc -c <"${path}" | tr -d ' '
  else
    echo "-1"
  fi
}

require_python3() {
  if ! command -v python3 >/dev/null 2>&1; then
    echo "缺少 python3，无法解析接口 JSON 响应。"
    exit 1
  fi
}

api_post() {
  local url="$1"
  local auth_header="$2"
  local body="${3:-}"

  if [[ -n "${body}" ]]; then
    curl -sS -X POST "${url}" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer ${auth_header}" \
      -d "${body}"
  else
    curl -sS -X POST "${url}" \
      -H "Authorization: Bearer ${auth_header}"
  fi
}

ensure_ok_response() {
  local resp="$1"
  local code
  code="$(api_response_code "${resp}")"
  if [[ "${code}" != "0" ]]; then
    echo "API 返回失败：code=${code}, message=$(api_response_message "${resp}")"
    return 2
  fi
  return 0
}

api_response_code() {
  local resp="$1"
  require_python3
  python3 - <<PY
import json
raw = r'''${resp}'''
try:
  data = json.loads(raw)
except Exception:
  print(-1)
  raise SystemExit(0)
print(data.get("code", -1))
PY
}

api_response_message() {
  local resp="$1"
  require_python3
  python3 - <<PY
import json
raw = r'''${resp}'''
try:
  data = json.loads(raw)
except Exception:
  print("(invalid json)")
  raise SystemExit(0)
print(data.get("message") or "")
PY
}

auto_select_video_id() {
  require_python3
  # Prefer public paging API in docker profile
  local resp
  resp="$(curl -sS "${API_BASE}/api/videos/page?sort=time&page=1&pageSize=1" || true)"
  python3 - <<PY
import json
import sys
raw = r'''${resp}'''
try:
  data = json.loads(raw)
except Exception:
  sys.exit(1)

items = ((data.get("data") or {}).get("items")) or []
if not items:
  sys.exit(1)

vid = items[0].get("id")
if not vid:
  sys.exit(1)
print(vid)
PY
}

resolve_kafka_consumer_cmd() {
  if command -v kafka-console-consumer >/dev/null 2>&1; then
    echo "kafka-console-consumer --bootstrap-server ${KAFKA_BOOTSTRAP} --topic ${KAFKA_TOPIC} --max-messages 1 --timeout-ms ${KAFKA_TIMEOUT_MS}"
    return 0
  fi

  local container
  if command -v docker >/dev/null 2>&1 && container="$(detect_kafka_container)"; then
    echo "docker exec -i ${container} bash -lc 'kafka-console-consumer --bootstrap-server kafka:9092 --topic ${KAFKA_TOPIC} --max-messages 1 --timeout-ms ${KAFKA_TIMEOUT_MS}'"
    return 0
  fi

  return 1
}

if [[ -z "${USER_TOKEN}" ]]; then
  if [[ -n "${ACCOUNT}" && -n "${PASSWORD}" ]]; then
    LOGIN_RESP="$(curl -s -X POST "${API_BASE}/api/auth/login" -H "Content-Type: application/json" -d "{\"account\":\"${ACCOUNT}\",\"password\":\"${PASSWORD}\"}")"
    require_python3
    USER_TOKEN="$(python3 - <<PY
import json
import sys
data = json.loads(r'''$LOGIN_RESP''')
token = (data.get("data") or {}).get("token") or data.get("token")
print(token or "")
PY
)"
    if [[ -z "${USER_TOKEN}" ]]; then
      echo "登录未拿到 token，请检查账号密码与登录响应："
      echo "${LOGIN_RESP}"
      exit 1
    fi
  else
    echo "请设置 USER_TOKEN 或提供 ACCOUNT/PASSWORD 用于登录。"
    exit 1
  fi
fi

if [[ -z "${VIDEO_ID}" ]]; then
  if VIDEO_ID="$(auto_select_video_id)"; then
    echo "未设置 VIDEO_ID，自动选择 VIDEO_ID=${VIDEO_ID}"
  else
    echo "未设置 VIDEO_ID，且无法从 ${API_BASE}/api/videos/page 自动获取。"
    echo "请手动设置一个存在的 VIDEO_ID，例如：export VIDEO_ID=7"
    exit 1
  fi
fi

CONSUMER_CMD=""
if CONSUMER_CMD="$(resolve_kafka_consumer_cmd)"; then
  echo "启动 Kafka consumer，等待新的 ${KAFKA_TOPIC} 消息（${KAFKA_TIMEOUT_MS}ms）..."
  OUT_FILE="$(mktemp)"
  # Start consumer first so we won't miss messages.
  bash -lc "${CONSUMER_CMD}" >"${OUT_FILE}" 2>&1 &
  CONSUMER_PID=$!
  # Give consumer a moment to join the group / subscribe.
  sleep 1
else
  echo "未检测到 kafka-console-consumer，也无法通过 docker 进入 Kafka 容器。"
  echo "你可以："
  echo "- 安装 Kafka CLI（kafka-console-consumer），或"
  echo "- 确认 Kafka 容器存在（默认尝试 kafka / shortvideo-recsys-kafka），或设置 KAFKA_CONTAINER"
  exit 1
fi

LOG_SIZE_BEFORE="$(file_size_bytes "${BEHAVIOR_LOG_FILE}")"

echo "触发播放事件..."
PLAY_RESP="$(api_post "${API_BASE}/api/videos/${VIDEO_ID}/play" "${USER_TOKEN}" '{"durationMs":1200,"isCompleted":true}')"
if ! ensure_ok_response "${PLAY_RESP}"; then
  CODE="$(api_response_code "${PLAY_RESP}")"
  if [[ "${CODE}" == "40401" ]]; then
    if ALT_VIDEO_ID="$(auto_select_video_id)"; then
      echo "VIDEO_ID=${VIDEO_ID} 不可用（40401），改用 VIDEO_ID=${ALT_VIDEO_ID} 重试..."
      VIDEO_ID="${ALT_VIDEO_ID}"
      PLAY_RESP="$(api_post "${API_BASE}/api/videos/${VIDEO_ID}/play" "${USER_TOKEN}" '{"durationMs":1200,"isCompleted":true}')"
      ensure_ok_response "${PLAY_RESP}"
    else
      echo "VIDEO_ID=${VIDEO_ID} 不可用（40401），且无法自动选择可用视频。"
      exit 2
    fi
  else
    exit 2
  fi
fi

echo "触发点赞事件..."
LIKE_RESP="$(api_post "${API_BASE}/api/videos/${VIDEO_ID}/like" "${USER_TOKEN}")"
ensure_ok_response "${LIKE_RESP}" || exit 2

# Give backend + flume a moment.
sleep 1

LOG_SIZE_AFTER="$(file_size_bytes "${BEHAVIOR_LOG_FILE}")"
if [[ "${LOG_SIZE_BEFORE}" != "-1" && "${LOG_SIZE_AFTER}" != "-1" && "${LOG_SIZE_AFTER}" -le "${LOG_SIZE_BEFORE}" ]]; then
  echo "警告：行为日志文件未增长：${BEHAVIOR_LOG_FILE} (${LOG_SIZE_BEFORE} -> ${LOG_SIZE_AFTER})"
  echo "这通常表示后端没有写到 Flume 采集路径。需要："
  echo "- 在启动后端前设置：export APP_BEHAVIOR_LOG_PATH=${BEHAVIOR_LOG_FILE}"
  echo "- 然后重启后端（确保环境变量生效）"
fi

wait "${CONSUMER_PID}" || true

OUT_CONTENT=""
if [[ -f "${OUT_FILE}" ]]; then
  OUT_CONTENT="$(cat "${OUT_FILE}")"
fi

if echo "${OUT_CONTENT}" | grep -Eq 'Processed a total of ([1-9][0-9]*) messages'; then
  echo "=== Kafka 消费到 1 条消息（验收通过） ==="
  echo "${OUT_CONTENT}"
  rm -f "${OUT_FILE}"
  exit 0
fi

echo "未在 ${KAFKA_TIMEOUT_MS}ms 内消费到新的 ${KAFKA_TOPIC} 消息（验收未通过）。"
if [[ -n "${OUT_CONTENT}" ]]; then
  echo "=== consumer 输出（用于排查） ==="
  echo "${OUT_CONTENT}"
fi
rm -f "${OUT_FILE}"

echo "排查建议："
echo "- 看后端是否写入 deploy/data/behavior/behavior-events.log"
echo "- 看 Flume 日志：docker logs --tail=200 shortvideo-recsys-flume"
echo "- 手动验证是否有历史消息："
if KAFKA_CONTAINER_DISPLAY="$(detect_kafka_container 2>/dev/null)"; then
  :
else
  KAFKA_CONTAINER_DISPLAY="shortvideo-recsys-kafka"
fi
echo "  docker exec -it ${KAFKA_CONTAINER_DISPLAY} bash -lc 'kafka-console-consumer --bootstrap-server kafka:9092 --topic ${KAFKA_TOPIC} --from-beginning --max-messages 1 --timeout-ms 15000'"
exit 2
