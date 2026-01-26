# 用途: 热榜验收脚本（PowerShell）
# 用法: powershell -File scripts/acceptance_hot_rank.ps1

param(
  [string]$BaseUrl = "http://127.0.0.1:8080",
  [string]$EnvFile = "deploy/.env",
  [switch]$StartCompose
)

$ErrorActionPreference = "Stop"

function Wait-Until([scriptblock]$Probe, [int]$TimeoutSec = 60, [int]$IntervalMs = 1000, [string]$What = "service") {
  $deadline = [DateTimeOffset]::UtcNow.AddSeconds($TimeoutSec)
  $lastError = $null
  while ([DateTimeOffset]::UtcNow -lt $deadline) {
    try {
      if (& $Probe) { return }
    } catch {
      $lastError = $_
    }
    Start-Sleep -Milliseconds $IntervalMs
  }
  if ($lastError) {
    throw "Timeout waiting for $What (>${TimeoutSec}s). Last error: $($lastError.Exception.Message)"
  }
  throw "Timeout waiting for $What (>${TimeoutSec}s)."
}

function Read-EnvFile([string]$path) {
  if (-not (Test-Path $path)) {
    $fallback = "deploy/.env.example"
    if (Test-Path $fallback) {
      $path = $fallback
    } else {
      throw "Env file not found: $EnvFile or deploy/.env.example"
    }
  }

  $vars = @{}
  Get-Content -Raw -Encoding UTF8 $path | ForEach-Object { $_ -split "`n" } | ForEach-Object {
    $line = $_.Trim()
    if ($line.Length -eq 0 -or $line.StartsWith("#")) { return }
    $idx = $line.IndexOf("=")
    if ($idx -le 0) { return }
    $k = $line.Substring(0, $idx).Trim()
    $v = $line.Substring($idx + 1).Trim()

    # Strip inline comments (dotenv style): VALUE # comment
    $v = ($v -replace '\s+#.*$','').Trim()

    $vars[$k] = $v
  }

  # Return PSCustomObject to simplify key access in different PowerShell environments.
  return [pscustomobject]$vars
}

$envs = Read-EnvFile $EnvFile

$composeProject = $envs.COMPOSE_PROJECT_NAME
if (-not $composeProject) { $composeProject = "shortvideo-recsys" }

$mysqlRootPass = $envs.MYSQL_ROOT_PASSWORD
if (-not $mysqlRootPass) { throw "Missing MYSQL_ROOT_PASSWORD (check $EnvFile)" }

$mysqlDb = $envs.MYSQL_DATABASE
if (-not $mysqlDb) { $mysqlDb = "shortvideo_recsys" }

$mysqlContainer = "$composeProject-mysql"

if ($StartCompose) {
  docker compose -f "deploy/docker-compose.yml" --env-file $EnvFile up -d | Out-Null
}

Wait-Until -What "MySQL container ($mysqlContainer)" -TimeoutSec 90 -Probe {
  $status = docker inspect -f "{{.State.Health.Status}}" $mysqlContainer 2>$null
  if ($LASTEXITCODE -ne 0) {
    throw "docker inspect failed for container: $mysqlContainer"
  }
  return ($status.Trim() -eq 'healthy')
}

Wait-Until -What "backend HTTP ($BaseUrl)" -TimeoutSec 90 -Probe {
  try {
    $r = Invoke-WebRequest -UseBasicParsing -Method Get -Uri "$BaseUrl/api/health" -TimeoutSec 2
    return ($r.StatusCode -ge 200 -and $r.StatusCode -lt 500)
  } catch {
    return $false
  }
}

$suffix = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$email = "rank$suffix@example.com"
$username = "rank$suffix"
$phone = "139" + ($suffix % 100000000).ToString("00000000")

$sqlLines = @(
  ("INSERT INTO users(username, phone, email, password_hash, status) VALUES ('{0}', '{1}', '{2}', 'x', 1);" -f $username, $phone, $email),
  "SET @uid = LAST_INSERT_ID();",
  "",
  "INSERT INTO videos(uploader_user_id, title, description, video_url, audit_status, is_hot) VALUES",
  ("(@uid, 'hot_a_{0}', 'demo', 'videos/hot_a_{0}.mp4', 'APPROVED', 0)," -f $suffix),
  ("(@uid, 'hot_b_{0}', 'demo', 'videos/hot_b_{0}.mp4', 'APPROVED', 0)," -f $suffix),
  ("(@uid, 'hot_c_{0}', 'demo', 'videos/hot_c_{0}.mp4', 'APPROVED', 0);" -f $suffix),
  "",
  "SET @v1 = LAST_INSERT_ID();",
  "SET @v2 = @v1 + 1;",
  "SET @v3 = @v1 + 2;",
  "",
  "INSERT INTO video_stats(video_id, play_count, like_count, comment_count, favorite_count, hot_score) VALUES",
  "(@v1, 1000, 2, 0, 0, 0),",
  "(@v2, 80, 30, 10, 5, 0),",
  "(@v3, 200, 5, 1, 1, 0);",
  "",
  "SELECT @v1 AS v1, @v2 AS v2, @v3 AS v3;"
)

$sql = $sqlLines -join "`n"

docker exec -i $mysqlContainer mysql -uroot "-p$mysqlRootPass" $mysqlDb -e $sql | Out-Null

function BasicAuthHeader([string]$user, [string]$pass) {
  $bytes = [System.Text.Encoding]::UTF8.GetBytes("$user`:$pass")
  $b64 = [Convert]::ToBase64String($bytes)
  return @{ Authorization = "Basic $b64" }
}

$adminUser = "admin"
$adminPass = "Admin123!"

Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/admin/rank/hot/refresh" -Headers (BasicAuthHeader $adminUser $adminPass) | Out-Null

$resp = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/rank/hot?page=1&pageSize=10"
$resp.data.items | Select-Object id,title,playCount,likeCount,commentCount,favoriteCount,hotScore | Format-Table -AutoSize

