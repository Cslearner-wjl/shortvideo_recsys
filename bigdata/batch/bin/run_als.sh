#!/usr/bin/env bash
# 用途: 运行 ALS 批处理任务
# 用法: bash bigdata/batch/bin/run_als.sh

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
  bigdata/batch/bin/run_als.sh [--spark-version]
  bigdata/batch/bin/run_als.sh [--dry-run]

环境变量（可选）:
  SPARK_DIST / SPARK_VERSION / SPARK_TGZ_URL / SPARK_CACHE_DIR
  SPARK_MASTER                  默认 local[*]
  JAR_PATH                      默认 bigdata/batch/target/batch-0.1.0.jar
  MAIN_CLASS                    默认 com.shortvideo.recsys.batch.AlsTrainJob
  AUTO_BUILD                    设为 1 时自动执行 mvn 构建

作业参数主要通过环境变量传入，详见 bigdata/batch/README.md。
EOF
  exit 0
fi

if [[ "${1:-}" == "--spark-version" ]]; then
  SPARK_DIST="$("${SCRIPT_DIR}/ensure_spark.sh")"
  SPARK_SUBMIT="${SPARK_DIST}/bin/spark-submit"
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

JAR_PATH="${JAR_PATH:-${MODULE_DIR}/target/batch-0.1.0.jar}"
if [[ ! -f "${JAR_PATH}" ]]; then
  echo "未找到 jar：${JAR_PATH}" >&2
  echo "请先构建：mvn -f bigdata/batch/pom.xml -DskipTests package（或设置 AUTO_BUILD=1）。" >&2
  exit 1
fi

SPARK_DIST="$("${SCRIPT_DIR}/ensure_spark.sh")"
SPARK_SUBMIT="${SPARK_DIST}/bin/spark-submit"
if [[ ! -x "${SPARK_SUBMIT}" ]]; then
  echo "未找到 spark-submit：${SPARK_SUBMIT}" >&2
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

MAIN_CLASS="${MAIN_CLASS:-com.shortvideo.recsys.batch.AlsTrainJob}"
SPARK_MASTER="${SPARK_MASTER:-local[*]}"

# 设置默认的 JDBC 连接参数（连接 Docker 中的 MySQL）
# 使用 127.0.0.1:3307（Docker 映射端口）
export JDBC_URL="${JDBC_URL:-jdbc:mysql://127.0.0.1:3307/shortvideo_recsys?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai}"
export JDBC_USER="${JDBC_USER:-app}"
export JDBC_PASSWORD="${JDBC_PASSWORD:-apppass}"
export JDBC_TABLE="${JDBC_TABLE:-user_actions}"

# 设置默认的 Redis 连接参数
export REDIS_HOST="${REDIS_HOST:-127.0.0.1}"
export REDIS_PORT="${REDIS_PORT:-6379}"

extra_args=()
if [[ -n "${SPARK_SUBMIT_EXTRA_ARGS:-}" ]]; then
  read -r -a extra_args <<< "${SPARK_SUBMIT_EXTRA_ARGS}"
fi

cmd=(
  "${SPARK_SUBMIT}"
  --class "${MAIN_CLASS}"
  --master "${SPARK_MASTER}"
  --conf "spark.driver.extraJavaOptions=${DRIVER_OPTS}"
  --conf "spark.executor.extraJavaOptions=${EXECUTOR_OPTS}"
  "${extra_args[@]}"
  "${JAR_PATH}"
)

if [[ "${DRY_RUN}" -eq 1 ]]; then
  printf '%q ' "${cmd[@]}"
  echo
  exit 0
fi

exec "${cmd[@]}"

