#!/usr/bin/env bash
# 用途: 从网站拉取视频并导入数据库
# 用法: bash scripts/crawl_and_import.sh

# =============================================================================
# 视频爬取与导入一体化脚本
# 功能：自动爬取视频（mock/pexels/pixabay）并通过管理端接口导入到短视频系统
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
CRAWLER_DIR="$PROJECT_ROOT/生成视频"
CRAWLER_SCRIPT="$CRAWLER_DIR/video_crawler.py"
IMPORT_SCRIPT="$SCRIPT_DIR/batch_import_videos.sh"

usage() {
  cat <<'USAGE'
用法:
  crawl_and_import.sh [选项]

视频来源选项:
  --source              视频来源: mock|pexels|pixabay (默认 mock)
  --api-key             Pexels/Pixabay API 密钥
  --query               搜索关键词 (默认 nature)
  --count               爬取视频数量 (默认 10)

导入选项:
  --base-url            后端地址 (默认 http://127.0.0.1:18080)
  --admin-user          管理端账号 (默认 admin)
  --admin-pass          管理端密码 (AdminPass123)
  --uploader-username   上传者用户名 (默认 uploaduser)
  --auto-approve        上传后自动审核 (默认开启)
  --no-auto-approve     关闭自动审核

其他选项:
  --download            同时下载视频文件到本地
  --output              JSON 输出文件名 (默认 videos_import.json)
  --dry-run             仅爬取数据，不导入
  --skip-crawl          跳过爬取，使用已有的 JSON 文件
  -h, --help            显示帮助

环境变量:
  BASE_URL, ADMIN_USER, ADMIN_PASS, PEXELS_API_KEY, PIXABAY_API_KEY

示例:
  # 1. 快速测试：爬取 10 个模拟视频并导入
  ./crawl_and_import.sh --source mock --count 10 --admin-pass Admin123!

  # 2. 使用 Pexels 获取自然风景视频
  ./crawl_and_import.sh --source pexels --api-key YOUR_KEY --query "nature" --count 20 --admin-pass Admin123!

  # 3. 使用 Pixabay 获取美食视频
  ./crawl_and_import.sh --source pixabay --api-key YOUR_KEY --query "food" --count 15 --admin-pass Admin123!

  # 4. 仅爬取不导入
  ./crawl_and_import.sh --source mock --count 10 --dry-run

  # 5. 使用已有 JSON 文件导入
  ./crawl_and_import.sh --skip-crawl --output existing_videos.json --admin-pass Admin123!
USAGE
}

log() { printf '\033[0;32m[INFO]\033[0m %s\n' "$*"; }
warn() { printf '\033[0;33m[WARN]\033[0m %s\n' "$*" >&2; }
fail() { printf '\033[0;31m[ERROR]\033[0m %s\n' "$*" >&2; exit 1; }

# 默认值
SOURCE="mock"
API_KEY="${PEXELS_API_KEY:-${PIXABAY_API_KEY:-}}"
QUERY="nature"
COUNT=10
BASE_URL="${BASE_URL:-http://127.0.0.1:18080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASS="${ADMIN_PASS:-}"
UPLOADER_USERNAME="${UPLOADER_USERNAME:-uploaduser}"
AUTO_APPROVE=1
DOWNLOAD=0
OUTPUT="videos_import.json"
DRY_RUN=0
SKIP_CRAWL=0

# 解析参数
while [[ $# -gt 0 ]]; do
  case "$1" in
    --source)       SOURCE="$2"; shift 2 ;;
    --api-key)      API_KEY="$2"; shift 2 ;;
    --query)        QUERY="$2"; shift 2 ;;
    --count)        COUNT="$2"; shift 2 ;;
    --base-url)     BASE_URL="$2"; shift 2 ;;
    --admin-user)   ADMIN_USER="$2"; shift 2 ;;
    --admin-pass)   ADMIN_PASS="$2"; shift 2 ;;
    --uploader-username) UPLOADER_USERNAME="$2"; shift 2 ;;
    --auto-approve) AUTO_APPROVE=1; shift ;;
    --no-auto-approve) AUTO_APPROVE=0; shift ;;
    --download)     DOWNLOAD=1; shift ;;
    --output)       OUTPUT="$2"; shift 2 ;;
    --dry-run)      DRY_RUN=1; shift ;;
    --skip-crawl)   SKIP_CRAWL=1; shift ;;
    -h|--help)      usage; exit 0 ;;
    *) fail "未知参数: $1" ;;
  esac
done

# 检查依赖
check_dependencies() {
  if ! command -v python3 >/dev/null 2>&1; then
    fail "缺少 python3，请先安装"
  fi
  
  if ! python3 -c "import requests" 2>/dev/null; then
    warn "Python requests 库未安装，正在安装..."
    pip3 install requests || fail "安装 requests 失败"
  fi
  
  if [[ $DRY_RUN -eq 0 ]]; then
    if ! command -v curl >/dev/null 2>&1; then
      fail "缺少 curl，请先安装"
    fi
    if ! command -v jq >/dev/null 2>&1; then
      fail "缺少 jq，请先安装"
    fi
  fi
}

# 检查爬虫脚本
check_crawler() {
  if [[ ! -f "$CRAWLER_SCRIPT" ]]; then
    fail "爬虫脚本不存在: $CRAWLER_SCRIPT"
  fi
}

# 检查导入脚本
check_import_script() {
  if [[ ! -f "$IMPORT_SCRIPT" ]]; then
    fail "导入脚本不存在: $IMPORT_SCRIPT"
  fi
}

# 转换爬虫输出格式为导入脚本格式
# 爬虫输出: videoUrl (远程URL)
# 导入脚本需要: filePath (本地文件路径)
# 所以需要先下载视频，再导入
convert_and_download() {
  local input_json="$1"
  local output_json="$2"
  local download_dir="$PROJECT_ROOT/生成视频/downloads"
  
  mkdir -p "$download_dir"
  
  log "正在下载视频文件..."
  
  local converted='[]'
  local count=0
  local total
  total=$(jq 'length' "$input_json")
  
  while IFS= read -r item; do
    local video_url title description tags id
    video_url=$(echo "$item" | jq -r '.videoUrl // empty')
    title=$(echo "$item" | jq -r '.title // empty')
    description=$(echo "$item" | jq -r '.description // empty')
    tags=$(echo "$item" | jq -r '.tags // empty')
    id=$(echo "$item" | jq -r '.id // empty')
    
    if [[ -z "$video_url" ]]; then
      warn "跳过无效条目（缺少 videoUrl）"
      continue
    fi
    
    count=$((count + 1))
    local filename="video_${id:-$count}.mp4"
    local filepath="$download_dir/$filename"
    
    log "[$count/$total] 下载: $title"
    
    if [[ -f "$filepath" ]]; then
      log "  文件已存在，跳过下载"
    else
      if curl --noproxy '*' -sS -L -o "$filepath" "$video_url" 2>/dev/null; then
        log "  ✓ 下载成功"
      else
        warn "  ✗ 下载失败: $video_url"
        continue
      fi
    fi
    
    # 构建导入格式的 JSON 项
    local tags_json
    if [[ "$tags" == \[*\] ]]; then
      tags_json="$tags"
    elif [[ -n "$tags" ]]; then
      # 逗号分隔转 JSON 数组
      tags_json=$(echo "$tags" | jq -R 'split(",") | map(gsub("^\\s+|\\s+$"; ""))')
    else
      tags_json='[]'
    fi
    
    local new_item
    new_item=$(jq -n \
      --arg path "$filepath" \
      --arg title "$title" \
      --arg desc "$description" \
      --argjson tags "$tags_json" \
      '{filePath: $path, title: $title, description: $desc, tags: $tags}')
    
    converted=$(echo "$converted" | jq --argjson item "$new_item" '. + [$item]')
    
  done < <(jq -c '.[]' "$input_json")
  
  echo "$converted" > "$output_json"
  log "转换完成，共 $count 个视频"
}

# 主流程
main() {
  log "=========================================="
  log "  视频爬取与导入一体化工具"
  log "=========================================="
  
  check_dependencies
  
  local crawl_output="$CRAWLER_DIR/$OUTPUT"
  local import_input="$CRAWLER_DIR/videos_for_import.json"
  
  # 步骤1：爬取视频数据
  if [[ $SKIP_CRAWL -eq 0 ]]; then
    check_crawler
    
    log ""
    log "[步骤1] 爬取视频数据"
    log "  来源: $SOURCE"
    log "  关键词: $QUERY"
    log "  数量: $COUNT"
    
    local crawler_args=(
      --source "$SOURCE"
      --query "$QUERY"
      --count "$COUNT"
      --output "$crawl_output"
    )
    
    if [[ -n "$API_KEY" ]]; then
      crawler_args+=(--api-key "$API_KEY")
    fi
    
    if [[ $DOWNLOAD -eq 1 ]]; then
      crawler_args+=(--download)
    fi
    
    cd "$CRAWLER_DIR"
    python3 video_crawler.py "${crawler_args[@]}"
    cd "$PROJECT_ROOT"
    
    if [[ ! -f "$crawl_output" ]]; then
      fail "爬取失败，未生成输出文件"
    fi
    
    log "爬取完成: $crawl_output"
  else
    crawl_output="$CRAWLER_DIR/$OUTPUT"
    if [[ ! -f "$crawl_output" ]]; then
      fail "指定的 JSON 文件不存在: $crawl_output"
    fi
    log "[步骤1] 跳过爬取，使用已有文件: $crawl_output"
  fi
  
  # 如果是 dry-run 模式，到此结束
  if [[ $DRY_RUN -eq 1 ]]; then
    log ""
    log "[完成] Dry-run 模式，视频数据已保存到: $crawl_output"
    log "可以使用以下命令查看:"
    log "  cat $crawl_output | jq '.[] | {title, videoUrl}'"
    exit 0
  fi
  
  # 步骤2：下载视频并转换格式
  log ""
  log "[步骤2] 下载视频文件并转换格式"
  
  convert_and_download "$crawl_output" "$import_input"
  
  # 步骤3：导入到系统
  if [[ -z "$ADMIN_PASS" ]]; then
    fail "管理端密码不能为空，请通过 --admin-pass 或 ADMIN_PASS 提供"
  fi
  
  check_import_script
  
  log ""
  log "[步骤3] 导入视频到系统"
  log "  后端地址: $BASE_URL"
  log "  管理员: $ADMIN_USER"
  log "  上传者: $UPLOADER_USERNAME"
  log "  自动审核: $AUTO_APPROVE"
  
  local import_args=(
    --input "$import_input"
    --base-url "$BASE_URL"
    --admin-user "$ADMIN_USER"
    --admin-pass "$ADMIN_PASS"
    --uploader-username "$UPLOADER_USERNAME"
  )
  
  if [[ $AUTO_APPROVE -eq 1 ]]; then
    import_args+=(--auto-approve)
  else
    import_args+=(--no-auto-approve)
  fi
  
  bash "$IMPORT_SCRIPT" "${import_args[@]}"
  
  log ""
  log "=========================================="
  log "  ✅ 全部完成！"
  log "=========================================="
  log "爬取数据: $crawl_output"
  log "导入数据: $import_input"
  log "视频文件: $CRAWLER_DIR/downloads/"
}

main
