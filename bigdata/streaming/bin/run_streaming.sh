#!/usr/bin/env bash
# 用途: 启动流式计算任务
# 用法: bash bigdata/streaming/bin/run_streaming.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODULE_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

append_if_missing() {
  local base="$1"
  local needle="$2"
  local add="$3"
  if [[ "${base}" == *"${needle}"* ]]; then
    echo "${base}"
  else
    echo "${base} ${add}"
  fi
}

if [[ "${1:-}" == "--help" ]]; then
  cat <<'EOF'
用法:
  bigdata/streaming/bin/run_streaming.sh [--spark-version]
  bigdata/streaming/bin/run_streaming.sh [--dry-run] [app_args...]

环境变量（可选）:
  SPARK_DIST                  指向已安装的 Spark 目录（跳过下载）
  SPARK_VERSION               默认 3.5.1
  SPARK_TGZ_URL               Spark tgz 下载地址（离线/镜像环境）
  SPARK_CACHE_DIR             默认 bigdata/streaming/.spark
  SPARK_MASTER                默认 local[*]
  JAR_PATH                    默认 bigdata/streaming/target/streaming-0.1.0.jar
  MAIN_CLASS                  默认 com.shortvideo.recsys.streaming.BehaviorStreamingJob
  AUTO_BUILD                  设为 1 时自动执行 mvn 构建
  SPARK_DRIVER_EXTRA_JAVA_OPTIONS / SPARK_EXECUTOR_EXTRA_JAVA_OPTIONS
EOF
  exit 0
fi

if [[ "${1:-}" == "--spark-version" ]]; then
  SPARK_DIST="$("${SCRIPT_DIR}/ensure_spark.sh")"
  SPARK_SUBMIT="${SPARK_DIST}/bin/spark-submit"
  if [[ ! -x "${SPARK_SUBMIT}" ]]; then
    echo "未找到 spark-submit：${SPARK_SUBMIT}" >&2
    exit 1
  fi

  SPARK_SUBMIT_OPTS="${SPARK_SUBMIT_OPTS:-}"
  SPARK_SUBMIT_OPTS="$(append_if_missing "${SPARK_SUBMIT_OPTS}" "java.base/java.nio" "--add-opens=java.base/java.nio=ALL-UNNAMED")"
  SPARK_SUBMIT_OPTS="$(append_if_missing "${SPARK_SUBMIT_OPTS}" "java.base/sun.nio.ch" "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED")"
  export SPARK_SUBMIT_OPTS

  exec "${SPARK_SUBMIT}" --version
fi

DRY_RUN=0
if [[ "${1:-}" == "--dry-run" ]]; then
  DRY_RUN=1
  shift
fi

if [[ "${AUTO_BUILD:-0}" == "1" ]]; then
  mvn -f "${MODULE_DIR}/pom.xml" -DskipTests package
fi

JAR_PATH="${JAR_PATH:-${MODULE_DIR}/target/streaming-0.1.0.jar}"
if [[ ! -f "${JAR_PATH}" ]]; then
  echo "未找到 jar：${JAR_PATH}" >&2
  echo "请先构建：mvn -f bigdata/streaming/pom.xml -DskipTests package（或设置 AUTO_BUILD=1）。" >&2
  exit 1
fi

SPARK_DIST="$("${SCRIPT_DIR}/ensure_spark.sh")"
SPARK_SUBMIT="${SPARK_DIST}/bin/spark-submit"
if [[ ! -x "${SPARK_SUBMIT}" ]]; then
  echo "未找到 spark-submit：${SPARK_SUBMIT}" >&2
  echo "请检查 SPARK_DIST 或确认 Spark 解压目录是否完整。" >&2
  exit 1
fi

DEFAULT_OPENS="--add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/sun.nio.ch=ALL-UNNAMED"

SPARK_SUBMIT_OPTS="${SPARK_SUBMIT_OPTS:-}"
SPARK_SUBMIT_OPTS="$(append_if_missing "${SPARK_SUBMIT_OPTS}" "java.base/java.nio" "--add-opens=java.base/java.nio=ALL-UNNAMED")"
SPARK_SUBMIT_OPTS="$(append_if_missing "${SPARK_SUBMIT_OPTS}" "java.base/sun.nio.ch" "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED")"
export SPARK_SUBMIT_OPTS

DRIVER_OPTS="${DEFAULT_OPENS} ${SPARK_DRIVER_EXTRA_JAVA_OPTIONS:-}"
EXECUTOR_OPTS="${DEFAULT_OPENS} ${SPARK_EXECUTOR_EXTRA_JAVA_OPTIONS:-}"
DRIVER_OPTS="$(echo "${DRIVER_OPTS}" | xargs)"
EXECUTOR_OPTS="$(echo "${EXECUTOR_OPTS}" | xargs)"

MAIN_CLASS="${MAIN_CLASS:-com.shortvideo.recsys.streaming.BehaviorStreamingJob}"
SPARK_MASTER="${SPARK_MASTER:-local[*]}"

extra_args=()
if [[ -n "${SPARK_SUBMIT_EXTRA_ARGS:-}" ]]; then
  read -r -a extra_args <<< "${SPARK_SUBMIT_EXTRA_ARGS}"
fi

# Kafka connector for Spark Structured Streaming
KAFKA_PACKAGE="${KAFKA_PACKAGE:-org.apache.spark:spark-sql-kafka-0-10_2.12:3.5.1}"

cmd=(
  "${SPARK_SUBMIT}"
  --class "${MAIN_CLASS}"
  --master "${SPARK_MASTER}"
  --packages "${KAFKA_PACKAGE}"
  --conf "spark.driver.extraJavaOptions=${DRIVER_OPTS}"
  --conf "spark.executor.extraJavaOptions=${EXECUTOR_OPTS}"
  "${extra_args[@]}"
  "${JAR_PATH}"
  "$@"
)

if [[ "${DRY_RUN}" -eq 1 ]]; then
  printf '%q ' "${cmd[@]}"
  echo
  exit 0
fi

exec "${cmd[@]}"
