#!/usr/bin/env bash
# JMeter 混合场景压测：读/点赞/评论/发评按 80/10/5/5 执行。
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
PLAN="$ROOT_DIR/tests/jmeter/backend_api.jmx"
OUT_DIR="$ROOT_DIR/docs/tests/jmeter"
TIMESTAMP="$(date +%Y%m%d%H%M%S)"

HOST="${HOST:-host.docker.internal}"
PORT="${PORT:-18080}"
PROTOCOL="${PROTOCOL:-http}"
LOGIN_ACCOUNT="${LOGIN_ACCOUNT:-}"
LOGIN_PASSWORD="${LOGIN_PASSWORD:-}"
JMETER_DOCKER_IMAGE="${JMETER_DOCKER_IMAGE:-justb4/jmeter:latest}"
THREADS="${THREADS:-100}"
RAMP="${RAMP:-10}"
LOOPS="${LOOPS:-20}"
CONNECT_TIMEOUT="${CONNECT_TIMEOUT:-5000}"
RESPONSE_TIMEOUT="${RESPONSE_TIMEOUT:-10000}"

if [[ -z "$LOGIN_ACCOUNT" || -z "$LOGIN_PASSWORD" ]]; then
  echo "缺少登录账号/密码，请设置 LOGIN_ACCOUNT 和 LOGIN_PASSWORD" >&2
  exit 1
fi

mkdir -p "$OUT_DIR"
JTL="$OUT_DIR/results_stage1_${TIMESTAMP}.jtl"
REPORT_DIR="$OUT_DIR/report_stage1_${TIMESTAMP}"

if command -v jmeter >/dev/null 2>&1; then
  JMETER_CMD=(jmeter)
elif command -v docker >/dev/null 2>&1; then
  JMETER_CMD=(docker run --rm -v "$ROOT_DIR/tests/jmeter":/test -v "$OUT_DIR":/results "$JMETER_DOCKER_IMAGE")
  PLAN="/test/backend_api.jmx"
  JTL="/results/results_stage1_${TIMESTAMP}.jtl"
  REPORT_DIR="/results/report_stage1_${TIMESTAMP}"
else
  echo "未找到 jmeter 或 docker，请先安装后再执行" >&2
  exit 1
fi

"${JMETER_CMD[@]}" -n -t "$PLAN" -l "$JTL" -e -o "$REPORT_DIR" \
  -Jhost="$HOST" -Jport="$PORT" -Jprotocol="$PROTOCOL" \
  -Jlogin_account="$LOGIN_ACCOUNT" -Jlogin_password="$LOGIN_PASSWORD" \
  -Jthreads="$THREADS" -Jramp="$RAMP" -Jloops="$LOOPS" \
  -Jscheduler=false -Jcontinue_forever=false \
  -Jconnect_timeout="$CONNECT_TIMEOUT" -Jresponse_timeout="$RESPONSE_TIMEOUT"
