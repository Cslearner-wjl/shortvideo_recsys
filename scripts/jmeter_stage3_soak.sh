#!/usr/bin/env bash
# JMeter 浸泡测试：使用摸高峰值 70% 并发运行 30+ 分钟。
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
PLAN="$ROOT_DIR/tests/jmeter/backend_api_peak_soak.jmx"
OUT_DIR="$ROOT_DIR/docs/tests/jmeter"
TIMESTAMP="$(date +%Y%m%d%H%M%S)"

HOST="${HOST:-host.docker.internal}"
PORT="${PORT:-18080}"
PROTOCOL="${PROTOCOL:-http}"
LOGIN_ACCOUNT="${LOGIN_ACCOUNT:-}"
LOGIN_PASSWORD="${LOGIN_PASSWORD:-}"
JMETER_DOCKER_IMAGE="${JMETER_DOCKER_IMAGE:-justb4/jmeter:latest}"
SOAK_THREADS="${SOAK_THREADS:-}"
SOAK_RATIO="${SOAK_RATIO:-0.7}"
RAMP="${RAMP:-10}"
DURATION="${DURATION:-1800}"
CONNECT_TIMEOUT="${CONNECT_TIMEOUT:-5000}"
RESPONSE_TIMEOUT="${RESPONSE_TIMEOUT:-10000}"
# 兼容旧变量：DOCKER_CONTAINER(单容器)；新变量：DOCKER_CONTAINERS(逗号分隔多容器)
DOCKER_CONTAINER="${DOCKER_CONTAINER:-}"
DOCKER_CONTAINERS="${DOCKER_CONTAINERS:-}"
BACKEND_PID="${BACKEND_PID:-}"

if [[ -z "$LOGIN_ACCOUNT" || -z "$LOGIN_PASSWORD" ]]; then
  echo "缺少登录账号/密码，请设置 LOGIN_ACCOUNT 和 LOGIN_PASSWORD" >&2
  exit 1
fi

mkdir -p "$OUT_DIR"

if [[ -z "$SOAK_THREADS" ]]; then
  SUMMARY="$OUT_DIR/peak_summary_latest.txt"
  if [[ ! -f "$SUMMARY" ]]; then
    echo "未找到摸高结果，请先执行 jmeter_stage2_peak.sh 或指定 SOAK_THREADS" >&2
    exit 1
  fi
  max_ok_threads=$(grep -E '^max_ok_threads=' "$SUMMARY" | awk -F= '{print $2}')
  if [[ -z "$max_ok_threads" || "$max_ok_threads" -le 0 ]]; then
    echo "摸高结果无效，请检查 $SUMMARY" >&2
    exit 1
  fi
  SOAK_THREADS=$(python3 - <<PY
import math
max_t = int("$max_ok_threads")
ratio = float("$SOAK_RATIO")
print(max(1, int(math.floor(max_t * ratio))))
PY
  )
fi

HOST_JTL="$OUT_DIR/results_stage3_${TIMESTAMP}.jtl"
HOST_REPORT="$OUT_DIR/report_stage3_${TIMESTAMP}"
HOST_STATS="$OUT_DIR/soak_docker_stats_${TIMESTAMP}.csv"
HOST_PROC_STATS="$OUT_DIR/soak_backend_proc_stats_${TIMESTAMP}.csv"

if command -v jmeter >/dev/null 2>&1; then
  JMETER_CMD=(jmeter)
  CONTAINER_MODE=false
  RUN_JTL="$HOST_JTL"
  RUN_REPORT="$HOST_REPORT"
  RUN_PLAN="$PLAN"
elif command -v docker >/dev/null 2>&1; then
  JMETER_CMD=(docker run --rm -v "$ROOT_DIR/tests/jmeter":/test -v "$OUT_DIR":/results "$JMETER_DOCKER_IMAGE")
  CONTAINER_MODE=true
  RUN_JTL="/results/results_stage3_${TIMESTAMP}.jtl"
  RUN_REPORT="/results/report_stage3_${TIMESTAMP}"
  RUN_PLAN="/test/backend_api_peak_soak.jmx"
else
  echo "未找到 jmeter 或 docker，请先安装后再执行" >&2
  exit 1
fi

stats_pid=""
proc_pid=""
cleanup() {
  if [[ -n "${stats_pid:-}" ]]; then
    kill "$stats_pid" >/dev/null 2>&1 || true
  fi
  if [[ -n "${proc_pid:-}" ]]; then
    kill "$proc_pid" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT INT TERM

if [[ -z "$DOCKER_CONTAINERS" && -n "$DOCKER_CONTAINER" ]]; then
  DOCKER_CONTAINERS="$DOCKER_CONTAINER"
fi

if [[ -n "$DOCKER_CONTAINERS" ]] && command -v docker >/dev/null 2>&1; then
  echo "timestamp,container,cpu_perc,mem_usage,mem_perc,net_io,block_io,pids" > "$HOST_STATS"
  IFS=',' read -r -a docker_containers <<< "$DOCKER_CONTAINERS"
  (while true; do
    docker stats --no-stream --format "{{.Name}},{{.CPUPerc}},{{.MemUsage}},{{.MemPerc}},{{.NetIO}},{{.BlockIO}},{{.PIDs}}" "${docker_containers[@]}" \
      | awk -v ts="$(date +%Y-%m-%dT%H:%M:%S)" '{print ts","$0}' >> "$HOST_STATS" || true
    sleep 30
  done) &
  stats_pid=$!
fi

if [[ -z "$BACKEND_PID" ]]; then
  # 后端可能在脚本启动时尚未开始监听，做一次短暂轮询以提高采样成功率。
  for _ in {1..30}; do
    BACKEND_PID="$(lsof -t -iTCP:"$PORT" -sTCP:LISTEN -n -P 2>/dev/null | head -n 1 || true)"
    if [[ -n "$BACKEND_PID" ]]; then
      break
    fi
    sleep 1
  done
fi

if [[ -n "$BACKEND_PID" ]]; then
  echo "timestamp,pid,cpu_perc,rss_kb,vsz_kb,etimes_s" > "$HOST_PROC_STATS"
  (while true; do
    ps -p "$BACKEND_PID" -o %cpu=,rss=,vsz=,etimes= \
      | awk -v ts="$(date +%Y-%m-%dT%H:%M:%S)" -v pid="$BACKEND_PID" '{print ts \",\" pid \",\" $1 \",\" $2 \",\" $3 \",\" $4}' >> "$HOST_PROC_STATS" || true
    sleep 30
  done) &
  proc_pid=$!
fi

"${JMETER_CMD[@]}" -n -t "$RUN_PLAN" -l "$RUN_JTL" -e -o "$RUN_REPORT" \
  -Jhost="$HOST" -Jport="$PORT" -Jprotocol="$PROTOCOL" \
  -Jlogin_account="$LOGIN_ACCOUNT" -Jlogin_password="$LOGIN_PASSWORD" \
  -Jthreads="$SOAK_THREADS" -Jramp="$RAMP" -Jduration="$DURATION" \
  -Jconnect_timeout="$CONNECT_TIMEOUT" -Jresponse_timeout="$RESPONSE_TIMEOUT"

echo "soak_threads=$SOAK_THREADS" > "$OUT_DIR/soak_summary_${TIMESTAMP}.txt"
