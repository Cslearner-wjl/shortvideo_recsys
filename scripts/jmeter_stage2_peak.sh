#!/usr/bin/env bash
# JMeter 摸高测试：从 100 线程起每分钟 +50，直到响应时间或错误率触发阈值停止。
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
START_THREADS="${START_THREADS:-100}"
STEP_THREADS="${STEP_THREADS:-50}"
RAMP="${RAMP:-10}"
DURATION="${DURATION:-60}"
THRESHOLD_RT_MS="${THRESHOLD_RT_MS:-500}"
THRESHOLD_ERR_RATE="${THRESHOLD_ERR_RATE:-0.01}"
CONNECT_TIMEOUT="${CONNECT_TIMEOUT:-5000}"
RESPONSE_TIMEOUT="${RESPONSE_TIMEOUT:-10000}"

if [[ -z "$LOGIN_ACCOUNT" || -z "$LOGIN_PASSWORD" ]]; then
  echo "缺少登录账号/密码，请设置 LOGIN_ACCOUNT 和 LOGIN_PASSWORD" >&2
  exit 1
fi

mkdir -p "$OUT_DIR"
CSV="$OUT_DIR/peak_results_${TIMESTAMP}.csv"
SUMMARY="$OUT_DIR/peak_summary_${TIMESTAMP}.txt"
LATEST="$OUT_DIR/peak_summary_latest.txt"

echo "threads,samples,errors,error_rate,avg_ms,p95_ms,rps,duration_s,status,jtl,report_dir" > "$CSV"

if command -v jmeter >/dev/null 2>&1; then
  JMETER_CMD=(jmeter)
  CONTAINER_MODE=false
elif command -v docker >/dev/null 2>&1; then
  JMETER_CMD=(docker run --rm -v "$ROOT_DIR/tests/jmeter":/test -v "$OUT_DIR":/results "$JMETER_DOCKER_IMAGE")
  CONTAINER_MODE=true
else
  echo "未找到 jmeter 或 docker，请先安装后再执行" >&2
  exit 1
fi

threads="$START_THREADS"
max_ok_threads=0
max_ok_rps=0
stop_reason=""

while true; do
  HOST_JTL="$OUT_DIR/results_stage2_${TIMESTAMP}_${threads}.jtl"
  HOST_REPORT="$OUT_DIR/report_stage2_${TIMESTAMP}_${threads}"
  if [[ "$CONTAINER_MODE" == "true" ]]; then
    RUN_JTL="/results/results_stage2_${TIMESTAMP}_${threads}.jtl"
    RUN_REPORT="/results/report_stage2_${TIMESTAMP}_${threads}"
    RUN_PLAN="/test/backend_api_peak_soak.jmx"
  else
    RUN_JTL="$HOST_JTL"
    RUN_REPORT="$HOST_REPORT"
    RUN_PLAN="$PLAN"
  fi

  "${JMETER_CMD[@]}" -n -t "$RUN_PLAN" -l "$RUN_JTL" -e -o "$RUN_REPORT" \
    -Jhost="$HOST" -Jport="$PORT" -Jprotocol="$PROTOCOL" \
    -Jlogin_account="$LOGIN_ACCOUNT" -Jlogin_password="$LOGIN_PASSWORD" \
    -Jthreads="$threads" -Jramp="$RAMP" -Jduration="$DURATION" \
    -Jconnect_timeout="$CONNECT_TIMEOUT" -Jresponse_timeout="$RESPONSE_TIMEOUT"

  read -r samples errors error_rate avg_ms p95_ms rps duration_s < <(
    python3 - <<PY
import csv, statistics
path = "$HOST_JTL"
with open(path, newline='') as f:
    reader = csv.DictReader(f)
    rows = []
    for r in reader:
        label = (r.get('label') or '').strip()
        # 仅统计真实业务 HTTP 请求，避免把 JSR223Sampler/登录/预热计入吞吐与延迟口径。
        if not (label.startswith('GET ') or label.startswith('POST ') or label.startswith('DELETE ')):
            continue
        if label.startswith('POST /api/auth/login') or label.endswith('(warmup)'):
            continue
        rows.append(r)
if not rows:
    print("0 0 0 0 0 0 0")
    raise SystemExit
samples = len(rows)
errors = sum(1 for r in rows if r.get('success', '').lower() == 'false')
error_rate = errors / samples if samples else 0
elapsed = [int(r.get('elapsed') or 0) for r in rows]
avg_ms = sum(elapsed) / samples if samples else 0
elapsed_sorted = sorted(elapsed)
idx = int((len(elapsed_sorted) - 1) * 0.95) if elapsed_sorted else 0
p95_ms = elapsed_sorted[idx] if elapsed_sorted else 0
start = min(int(r.get('timeStamp') or 0) for r in rows)
end = max(int(r.get('timeStamp') or 0) for r in rows)
duration_s = max(1, (end - start) / 1000)
rps = samples / duration_s if duration_s else 0
print(samples, errors, error_rate, avg_ms, p95_ms, rps, duration_s)
PY
  )

  status="ok"
  if python3 - <<PY
error_rate=float("$error_rate")
p95=float("$p95_ms")
threshold_err=float("$THRESHOLD_ERR_RATE")
threshold_rt=float("$THRESHOLD_RT_MS")
if error_rate > threshold_err or p95 > threshold_rt:
    raise SystemExit(1)
PY
  then
    max_ok_threads="$threads"
    max_ok_rps="$rps"
  else
    status="stop"
    if python3 - <<PY
error_rate=float("$error_rate")
threshold_err=float("$THRESHOLD_ERR_RATE")
if error_rate > threshold_err:
    raise SystemExit(0)
raise SystemExit(1)
PY
    then
      stop_reason="error_rate"
    else
      stop_reason="p95_over_500"
    fi
  fi

  echo "$threads,$samples,$errors,$error_rate,$avg_ms,$p95_ms,$rps,$duration_s,$status,$HOST_JTL,$HOST_REPORT" >> "$CSV"

  if [[ "$status" == "stop" ]]; then
    break
  fi
  threads=$((threads + STEP_THREADS))
  if [[ "$threads" -gt 2000 ]]; then
    stop_reason="max_threads_guard"
    break
  fi
  sleep 1
 done

echo "max_ok_threads=$max_ok_threads" > "$SUMMARY"
echo "max_ok_rps=$max_ok_rps" >> "$SUMMARY"
echo "stop_reason=$stop_reason" >> "$SUMMARY"
echo "results_csv=$CSV" >> "$SUMMARY"
cp "$SUMMARY" "$LATEST"

printf "max_ok_threads=%s max_ok_rps=%s stop_reason=%s\n" "$max_ok_threads" "$max_ok_rps" "$stop_reason"
