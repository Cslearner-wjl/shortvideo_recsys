#!/usr/bin/env bash
# 用途: 运行 M4 ALS 批处理任务
# 用法: bash scripts/run_m4_als_batch.sh

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
exec "${REPO_ROOT}/bigdata/batch/bin/run_als.sh" "$@"

