#!/usr/bin/env bash
# 用途: 检查 Spark 环境并准备运行依赖
# 用法: bash bigdata/batch/bin/ensure_spark.sh

set -euo pipefail

# 复用 streaming 模块的 Spark 下载/缓存逻辑，避免重复维护。
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
exec "${REPO_ROOT}/bigdata/streaming/bin/ensure_spark.sh"

