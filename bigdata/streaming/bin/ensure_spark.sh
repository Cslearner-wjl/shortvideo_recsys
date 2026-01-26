# 用途: 检查 Spark 环境并准备流式任务依赖
# 用法: bash bigdata/streaming/bin/ensure_spark.sh


#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODULE_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

SPARK_VERSION="${SPARK_VERSION:-3.5.1}"
SPARK_PACKAGE="${SPARK_PACKAGE:-spark-${SPARK_VERSION}-bin-hadoop3}"
SPARK_CACHE_DIR="${SPARK_CACHE_DIR:-${MODULE_DIR}/.spark}"

if [[ -n "${SPARK_DIST:-}" ]]; then
  echo "${SPARK_DIST}"
  exit 0
fi

mkdir -p "${SPARK_CACHE_DIR}"

SPARK_DIST_DIR="${SPARK_CACHE_DIR}/${SPARK_PACKAGE}"
SPARK_TGZ_PATH="${SPARK_CACHE_DIR}/${SPARK_PACKAGE}.tgz"
DEFAULT_TGZ_URL_PRIMARY="https://archive.apache.org/dist/spark/spark-${SPARK_VERSION}/${SPARK_PACKAGE}.tgz"
DEFAULT_TGZ_URL_FALLBACK="https://downloads.apache.org/spark/spark-${SPARK_VERSION}/${SPARK_PACKAGE}.tgz"

SPARK_TGZ_URL_USER="${SPARK_TGZ_URL:-}"
SPARK_TGZ_SHA512_URL_USER="${SPARK_TGZ_SHA512_URL:-}"

SPARK_TGZ_URL="${SPARK_TGZ_URL_USER:-${DEFAULT_TGZ_URL_PRIMARY}}"
SPARK_TGZ_SHA512_URL="${SPARK_TGZ_SHA512_URL_USER:-${SPARK_TGZ_URL}.sha512}"

download_file() {
  local url="$1"
  local out="$2"

  if command -v curl >/dev/null 2>&1; then
    if [[ -f "${out}" ]]; then
      curl -fL --retry 3 --retry-all-errors --continue-at - "${url}" -o "${out}"
    else
      curl -fL --retry 3 --retry-all-errors "${url}" -o "${out}"
    fi
    return 0
  fi
  if command -v wget >/dev/null 2>&1; then
    if [[ -f "${out}" ]]; then
      wget -qcO "${out}" "${url}"
    else
      wget -qO "${out}" "${url}"
    fi
    return 0
  fi

  echo "缺少下载工具：请安装 curl 或 wget。" >&2
  return 1
}

sha512_check_file() {
  local file_path="$1"
  local sha512_file_path="$2"

  local expected actual
  expected="$(tr -d '\r' < "${sha512_file_path}" | grep -Eo '[0-9a-fA-F]{128}' | head -n 1 || true)"
  if [[ -z "${expected}" ]]; then
    echo "无法从 SHA512 文件解析 hash，跳过完整性校验：${sha512_file_path}" >&2
    return 2
  fi

  if command -v sha512sum >/dev/null 2>&1; then
    actual="$(sha512sum "${file_path}" | awk '{print $1}')"
    [[ "${expected,,}" == "${actual,,}" ]]
    return $?
  fi

  if command -v shasum >/dev/null 2>&1; then
    actual="$(shasum -a 512 "${file_path}" | awk '{print $1}')"
    [[ "${expected,,}" == "${actual,,}" ]]
    return $?
  fi

  echo "缺少 SHA512 校验工具（sha512sum/shasum），跳过完整性校验。" >&2
  return 2
}

download_spark_tgz() {
  local tgz_urls=()
  local sha_urls=()

  if [[ -n "${SPARK_TGZ_URL_USER}" ]]; then
    tgz_urls+=("${SPARK_TGZ_URL_USER}")
    sha_urls+=("${SPARK_TGZ_SHA512_URL_USER:-${SPARK_TGZ_URL_USER}.sha512}")
  else
    tgz_urls+=("${DEFAULT_TGZ_URL_PRIMARY}" "${DEFAULT_TGZ_URL_FALLBACK}")
    sha_urls+=("${DEFAULT_TGZ_URL_PRIMARY}.sha512" "${DEFAULT_TGZ_URL_FALLBACK}.sha512")
  fi

  local downloaded=0
  for i in "${!tgz_urls[@]}"; do
    local url="${tgz_urls[$i]}"
    local sha_url="${sha_urls[$i]}"
    echo "下载 Spark: ${url}" >&2
    if download_file "${url}" "${SPARK_TGZ_PATH}"; then
      SPARK_TGZ_URL="${url}"
      SPARK_TGZ_SHA512_URL="${sha_url}"
      downloaded=1
      break
    fi
  done

  if [[ "${downloaded}" -ne 1 ]]; then
    echo "下载失败：请设置 SPARK_TGZ_URL（或手动设置 SPARK_DIST）。" >&2
    return 1
  fi
}

if [[ -d "${SPARK_DIST_DIR}" ]] && [[ ! -x "${SPARK_DIST_DIR}/bin/spark-submit" ]]; then
  echo "检测到不完整的 Spark 目录，尝试清理后重新安装：${SPARK_DIST_DIR}" >&2
  rm -rf "${SPARK_DIST_DIR}" || true
fi

if [[ ! -d "${SPARK_DIST_DIR}" ]]; then
  if [[ ! -f "${SPARK_TGZ_PATH}" ]]; then
    download_spark_tgz || exit 1
  fi

  tmp_sha="${SPARK_TGZ_PATH}.sha512"
  if [[ ! -f "${tmp_sha}" ]]; then
    if download_file "${SPARK_TGZ_SHA512_URL}" "${tmp_sha}" 2>/dev/null; then
      :
    else
      rm -f "${tmp_sha}" || true
    fi
  fi

  if [[ -f "${tmp_sha}" ]]; then
    verify_once() {
      set +e
      sha512_check_file "${SPARK_TGZ_PATH}" "${tmp_sha}"
      rc=$?
      set -e
      echo "${rc}"
    }

    rc="$(verify_once)"
    if [[ "${rc}" -eq 1 ]]; then
      echo "Spark tgz SHA512 校验失败，尝试重新下载：${SPARK_TGZ_PATH}" >&2
      rm -f "${SPARK_TGZ_PATH}" "${tmp_sha}" || true
      download_spark_tgz || exit 1
      download_file "${SPARK_TGZ_SHA512_URL}" "${tmp_sha}" 2>/dev/null || rm -f "${tmp_sha}" || true

      if [[ -f "${tmp_sha}" ]]; then
        rc="$(verify_once)"
        if [[ "${rc}" -eq 1 ]]; then
          echo "Spark tgz SHA512 校验仍失败：${SPARK_TGZ_PATH}" >&2
          echo "请更换下载源（SPARK_TGZ_URL/SPARK_TGZ_SHA512_URL）或手动设置 SPARK_DIST。" >&2
          exit 1
        fi
      fi
    fi
  fi

  echo "解压 Spark 到: ${SPARK_DIST_DIR}" >&2
  set +e
  tar -xzf "${SPARK_TGZ_PATH}" -C "${SPARK_CACHE_DIR}"
  tar_rc=$?
  set -e
  if [[ "${tar_rc}" -ne 0 ]]; then
    echo "Spark 解压失败（可能是下载文件损坏）：${SPARK_TGZ_PATH}" >&2
    rm -f "${SPARK_TGZ_PATH}" || true
    rm -rf "${SPARK_DIST_DIR}" || true
    exit 1
  fi

  if [[ ! -x "${SPARK_DIST_DIR}/bin/spark-submit" ]]; then
    echo "Spark 目录不完整（缺少 bin/spark-submit）：${SPARK_DIST_DIR}" >&2
    echo "请更换下载源（SPARK_TGZ_URL）或手动设置 SPARK_DIST。" >&2
    rm -f "${SPARK_TGZ_PATH}" || true
    rm -rf "${SPARK_DIST_DIR}" || true
    exit 1
  fi
fi

echo "${SPARK_DIST_DIR}"
