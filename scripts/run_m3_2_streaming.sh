#!/usr/bin/env bash
# 用途: 启动 M3.2 流式任务
# 用法: bash scripts/run_m3_2_streaming.sh

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
exec "${REPO_ROOT}/bigdata/streaming/bin/run_streaming.sh" "$@"

