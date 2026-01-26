#!/usr/bin/env bash
# 用途: 批量导入视频数据
# 用法: bash scripts/batch_import_videos.sh

# 批量导入视频脚本：读取 CSV/JSON 并调用管理端接口上传视频，必要时自动审核。

set -euo pipefail

SCRIPT_NAME=$(basename "$0")

usage() {
  cat <<'USAGE'
用法:
  batch_import_videos.sh --input <文件> [--format csv|json] [选项]

必填参数:
  --input                 CSV/JSON 输入文件路径

可选参数:
  --format                csv 或 json（默认按扩展名识别）
  --base-url              后端地址（默认 http://127.0.0.1:8080）
  --admin-user            管理端账号（默认 admin）
  --admin-pass            管理端密码（必填，无默认值）
  --uploader-username     作为 uploaderUserId 的用户名（默认 uploaduser）
  --uploader-id           直接指定 uploaderUserId（优先级最高）
  --auto-approve          上传后自动审核为 APPROVED（默认开启）
  --no-auto-approve       关闭自动审核
  --root                  处理相对路径时的根目录（默认：输入文件所在目录）
  --dry-run               仅打印计划，不执行上传
  -h, --help              显示帮助

环境变量:
  BASE_URL, ADMIN_USER, ADMIN_PASS(必填), UPLOADER_USERNAME, UPLOADER_ID, AUTO_APPROVE, ROOT_DIR

输入格式:
  CSV: file_path,title,description,tags
    - title/tags 为空时，将从文件名解析
    - tags 支持 JSON 数组字符串或逗号分隔
  JSON: [{"filePath":"/path/xx.mp4","title":"","description":"","tags":["a","b"]}]

说明:
  1) 上传与审核使用管理端 Basic Auth（/api/admin/**），请提供 ADMIN_PASS。
  2) uploaduser 通过 Flyway 迁移预置，仅在 docker/test 环境建议使用。
  3) 脚本依赖 jq 解析 JSON 响应与输入。
USAGE
}

log() {
  printf '[INFO] %s\n' "$*"
}

warn() {
  printf '[WARN] %s\n' "$*" >&2
}

fail() {
  printf '[ERROR] %s\n' "$*" >&2
  exit 1
}

trim() {
  local s="$1"
  s="${s#"${s%%[![:space:]]*}"}"
  s="${s%"${s##*[![:space:]]}"}"
  printf '%s' "$s"
}

json_escape() {
  local s="$1"
  s=${s//\\/\\\\}
  s=${s//\"/\\\"}
  printf '%s' "$s"
}

json_query() {
  local expr="$1"
  local data="$2"
  local out
  set +e
  out=$(echo "$data" | jq -r "$expr" 2>/dev/null)
  local rc=$?
  set -e
  if [[ $rc -ne 0 ]]; then
    printf ''
    return 1
  fi
  printf '%s' "$out"
}

derive_title() {
  local path="$1"
  local base
  base=$(basename "$path")
  base=${base%.*}
  base=${base//_/ }
  base=${base//-/ }
  base=$(echo "$base" | tr -s ' ')
  base=$(trim "$base")
  printf '%s' "$base"
}

build_tags_from_name() {
  local path="$1"
  local base
  base=$(basename "$path")
  base=${base%.*}
  base=${base//_/ }
  base=${base//-/ }
  base=$(trim "$base")
  if [[ -z "$base" ]]; then
    printf ''
    return
  fi
  local json='['
  local first=1
  local token
  for token in $base; do
    token=$(trim "$token")
    if [[ -z "$token" ]]; then
      continue
    fi
    token=$(json_escape "$token")
    if [[ $first -eq 1 ]]; then
      json+="\"$token\""
      first=0
    else
      json+=",\"$token\""
    fi
  done
  json+=']'
  printf '%s' "$json"
}

build_tags_from_string() {
  local raw="$1"
  raw=$(trim "$raw")
  if [[ -z "$raw" ]]; then
    printf ''
    return
  fi
  if [[ "$raw" == \[*\] ]]; then
    printf '%s' "$raw"
    return
  fi
  local json='['
  local first=1
  local token
  IFS=',' read -r -a parts <<< "$raw"
  for token in "${parts[@]}"; do
    token=$(trim "$token")
    if [[ -z "$token" ]]; then
      continue
    fi
    token=$(json_escape "$token")
    if [[ $first -eq 1 ]]; then
      json+="\"$token\""
      first=0
    else
      json+=",\"$token\""
    fi
  done
  json+=']'
  printf '%s' "$json"
}

need_jq=1

BASE_URL=${BASE_URL:-http://127.0.0.1:8080}
ADMIN_USER=${ADMIN_USER:-admin}
ADMIN_PASS=${ADMIN_PASS:-}
UPLOADER_USERNAME=${UPLOADER_USERNAME:-uploaduser}
UPLOADER_ID=${UPLOADER_ID:-}
AUTO_APPROVE=${AUTO_APPROVE:-1}
FORMAT=${FORMAT:-}
ROOT_DIR=${ROOT_DIR:-}
INPUT_FILE=""
DRY_RUN=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --input)
      INPUT_FILE="$2"
      shift 2
      ;;
    --format)
      FORMAT="$2"
      shift 2
      ;;
    --base-url)
      BASE_URL="$2"
      shift 2
      ;;
    --admin-user)
      ADMIN_USER="$2"
      shift 2
      ;;
    --admin-pass)
      ADMIN_PASS="$2"
      shift 2
      ;;
    --uploader-username)
      UPLOADER_USERNAME="$2"
      shift 2
      ;;
    --uploader-id)
      UPLOADER_ID="$2"
      shift 2
      ;;
    --auto-approve)
      AUTO_APPROVE=1
      shift
      ;;
    --no-auto-approve)
      AUTO_APPROVE=0
      shift
      ;;
    --root)
      ROOT_DIR="$2"
      shift 2
      ;;
    --dry-run)
      DRY_RUN=1
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      fail "未知参数: $1"
      ;;
  esac
done

if [[ -z "$INPUT_FILE" ]]; then
  usage
  fail "缺少 --input"
fi

if [[ ! -f "$INPUT_FILE" ]]; then
  fail "输入文件不存在: $INPUT_FILE"
fi

if [[ -z "$FORMAT" ]]; then
  case "$INPUT_FILE" in
    *.csv|*.CSV)
      FORMAT="csv"
      ;;
    *.json|*.JSON)
      FORMAT="json"
      ;;
    *)
      fail "无法识别输入格式，请指定 --format csv|json"
      ;;
  esac
fi

if [[ -z "$ROOT_DIR" ]]; then
  ROOT_DIR=$(cd "$(dirname "$INPUT_FILE")" && pwd)
fi

if ! command -v curl >/dev/null 2>&1; then
  fail "缺少 curl，请先安装"
fi

if [[ "$FORMAT" == "json" ]]; then
  need_jq=1
fi

if [[ -z "$UPLOADER_ID" ]]; then
  need_jq=1
fi

if [[ "$AUTO_APPROVE" -eq 1 ]]; then
  need_jq=1
fi

if [[ $need_jq -eq 1 ]] && ! command -v jq >/dev/null 2>&1; then
  fail "脚本依赖 jq，请先安装（用于解析接口响应与输入数据）"
fi

if [[ -z "$ADMIN_PASS" ]]; then
  fail "管理端密码不能为空，请通过 --admin-pass 或 ADMIN_PASS 提供"
fi

fetch_uploader_id() {
  local resp
  resp=$(curl --noproxy '*' -sS -u "$ADMIN_USER:$ADMIN_PASS" "$BASE_URL/api/admin/users?page=1&size=50&keyword=$UPLOADER_USERNAME")
  local code
  code=$(json_query '.code' "$resp") || code=""
  if [[ -z "$code" ]]; then
    fail "查询 uploaderUserId 失败：管理端响应非 JSON，请检查地址/鉴权"
  fi
  if [[ "$code" != "0" ]]; then
    local msg
    msg=$(json_query '.message' "$resp") || msg=""
    fail "查询 uploaderUserId 失败: ${msg:-未知错误}"
  fi
  local id
  id=$(echo "$resp" | jq -r --arg u "$UPLOADER_USERNAME" '.data.items[] | select(.username==$u) | .id' | head -n1)
  if [[ -z "$id" || "$id" == "null" ]]; then
    fail "未找到用户名为 $UPLOADER_USERNAME 的用户，请确认已执行迁移"
  fi
  printf '%s' "$id"
}

if [[ -z "$UPLOADER_ID" ]]; then
  UPLOADER_ID=$(fetch_uploader_id)
  log "使用 uploaderUserId=$UPLOADER_ID (username=$UPLOADER_USERNAME)"
fi

resolve_path() {
  local path="$1"
  path=$(trim "$path")
  path=${path%$'\r'}
  if [[ -z "$path" ]]; then
    printf ''
    return
  fi
  if [[ "$path" = /* ]]; then
    printf '%s' "$path"
  else
    printf '%s/%s' "$ROOT_DIR" "$path"
  fi
}

upload_one() {
  local file_path="$1"
  local title="$2"
  local description="$3"
  local tags_json="$4"

  if [[ $DRY_RUN -eq 1 ]]; then
    log "DRY RUN: $file_path | $title"
    return
  fi

  local curl_args
  curl_args=(
    --noproxy '*'
    -sS
    -u "$ADMIN_USER:$ADMIN_PASS"
    -X POST
    "$BASE_URL/api/admin/videos"
    -F "uploaderUserId=$UPLOADER_ID"
    -F "title=$title"
    -F "video=@$file_path"
  )

  if [[ -n "$description" ]]; then
    curl_args+=( -F "description=$description" )
  fi

  if [[ -n "$tags_json" ]]; then
    curl_args+=( -F "tags=$tags_json" )
  fi

  local resp
  resp=$(curl "${curl_args[@]}")

  local code
  code=$(json_query '.code' "$resp") || code=""
  if [[ -z "$code" ]]; then
    warn "上传失败: $file_path | 管理端响应非 JSON，请检查地址/鉴权"
    return
  fi
  if [[ "$code" != "0" ]]; then
    local msg
    msg=$(json_query '.message' "$resp") || msg=""
    warn "上传失败: $file_path | ${msg:-未知错误}"
    return
  fi

  local video_id
  video_id=$(json_query '.data.id // empty' "$resp") || video_id=""
  if [[ -z "$video_id" || "$video_id" == "null" ]]; then
    warn "上传成功但未解析到 videoId: $file_path"
    return
  fi

  log "上传成功: $file_path -> id=$video_id"

  if [[ "$AUTO_APPROVE" -eq 1 ]]; then
    local audit_resp
    audit_resp=$(curl --noproxy '*' -sS -u "$ADMIN_USER:$ADMIN_PASS" -X PATCH \
      "$BASE_URL/api/admin/videos/$video_id/audit" \
      -H 'Content-Type: application/json' \
      -d '{"status":"APPROVED"}')
    local audit_code
    audit_code=$(json_query '.code' "$audit_resp") || audit_code=""
    if [[ -z "$audit_code" ]]; then
      warn "自动审核失败: id=$video_id | 管理端响应非 JSON，请检查地址/鉴权"
      return
    fi
    if [[ "$audit_code" != "0" ]]; then
      local msg
      msg=$(json_query '.message' "$audit_resp") || msg=""
      warn "自动审核失败: id=$video_id | ${msg:-未知错误}"
      return
    fi
    log "已审核通过: id=$video_id"
  fi
}

process_csv() {
  local line_no=0
  while IFS=',' read -r file_path title description tags || [[ -n "$file_path" ]]; do
    line_no=$((line_no + 1))
    file_path=$(trim "${file_path:-}")
    title=$(trim "${title:-}")
    description=$(trim "${description:-}")
    tags=$(trim "${tags:-}")

    if [[ $line_no -eq 1 ]]; then
      local header
      header=$(echo "$file_path" | tr '[:upper:]' '[:lower:]')
      if [[ "$header" == "file" || "$header" == "file_path" || "$header" == "path" ]]; then
        continue
      fi
    fi

    if [[ -z "$file_path" ]]; then
      continue
    fi

    local resolved
    resolved=$(resolve_path "$file_path")
    if [[ ! -f "$resolved" ]]; then
      warn "文件不存在，已跳过: $resolved"
      continue
    fi

    if [[ -z "$title" ]]; then
      title=$(derive_title "$resolved")
    fi

    local tags_json
    if [[ -n "$tags" ]]; then
      tags_json=$(build_tags_from_string "$tags")
    else
      tags_json=$(build_tags_from_name "$resolved")
    fi

    upload_one "$resolved" "$title" "$description" "$tags_json"
  done < "$INPUT_FILE"
}

process_json() {
  local item
  while IFS= read -r item; do
    local file_path
    file_path=$(echo "$item" | jq -r '.filePath // .path // empty')
    file_path=$(trim "$file_path")
    if [[ -z "$file_path" ]]; then
      warn "缺少 filePath，已跳过"
      continue
    fi

    local title
    title=$(echo "$item" | jq -r '.title // empty')
    title=$(trim "$title")

    local description
    description=$(echo "$item" | jq -r '.description // empty')
    description=$(trim "$description")

    local tags_json
    tags_json=$(echo "$item" | jq -c '.tags // empty')
    if [[ "$tags_json" == "null" || -z "$tags_json" ]]; then
      tags_json=""
    elif [[ "$tags_json" == \"*\" ]]; then
      local tags_raw
      tags_raw=$(echo "$item" | jq -r '.tags')
      tags_json=$(build_tags_from_string "$tags_raw")
    fi

    local resolved
    resolved=$(resolve_path "$file_path")
    if [[ ! -f "$resolved" ]]; then
      warn "文件不存在，已跳过: $resolved"
      continue
    fi

    if [[ -z "$title" ]]; then
      title=$(derive_title "$resolved")
    fi

    if [[ -z "$tags_json" ]]; then
      tags_json=$(build_tags_from_name "$resolved")
    fi

    upload_one "$resolved" "$title" "$description" "$tags_json"
  done < <(jq -c '.[]' "$INPUT_FILE")
}

log "输入文件: $INPUT_FILE"
log "后端地址: $BASE_URL"
log "导入格式: $FORMAT"
log "自动审核: $AUTO_APPROVE"

if [[ "$FORMAT" == "csv" ]]; then
  process_csv
elif [[ "$FORMAT" == "json" ]]; then
  process_json
else
  fail "不支持的格式: $FORMAT"
fi
