# 用途: 演示推荐脚本（PowerShell）
# 用法: powershell -File scripts/demo_recommendations.ps1

param(
  [string]$BaseUrl = "http://127.0.0.1:8080",
  [string]$EnvFile = "deploy/.env",
  [switch]$StartCompose,
  [string]$AdminUser = "admin",
  [string]$AdminPass = "AdminPass123"
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

function BasicAuthHeader([string]$user, [string]$pass) {
  $bytes = [System.Text.Encoding]::UTF8.GetBytes("$user`:$pass")
  $b64 = [Convert]::ToBase64String($bytes)
  return @{ Authorization = "Basic $b64" }
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
$email = "reco$suffix@example.com"
$username = "reco$suffix"
$phone = "139" + ($suffix % 100000000).ToString("00000000")
$password = "Passw0rd!"

Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/email-code" -ContentType "application/json" -Body (@{ email = $email } | ConvertTo-Json) | Out-Null

$code = (docker exec -i $mysqlContainer mysql -uroot "-p$mysqlRootPass" $mysqlDb -N -B -e "SELECT code FROM email_verification_codes WHERE email='$email' ORDER BY created_at DESC LIMIT 1;").Trim()
if (-not $code) { throw "Email verification code not found (ensure backend is running and using the same MySQL)." }

$reg = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/register" -ContentType "application/json" -Body (@{
  username = $username
  phone = $phone
  email = $email
  password = $password
  emailCode = $code
} | ConvertTo-Json)

$token = $reg.data.token
if (-not $token) { throw "Register failed: token is missing" }

$userId = (docker exec -i $mysqlContainer mysql -uroot "-p$mysqlRootPass" $mysqlDb -N -B -e "SELECT id FROM users WHERE email='$email' LIMIT 1;").Trim()
if (-not $userId) { throw "UserId not found" }

$sqlLines = @(
  "INSERT INTO videos(uploader_user_id, title, description, video_url, tags, audit_status, is_hot) VALUES",
  ('({0}, ''sports_seen_{1}'', ''demo'', ''videos/sports_seen_{1}.mp4'', JSON_ARRAY(''sports''), ''APPROVED'', 0),' -f $userId, $suffix),
  ('({0}, ''sports_cand_{1}'', ''demo'', ''videos/sports_cand_{1}.mp4'', JSON_ARRAY(''sports''), ''APPROVED'', 0),' -f $userId, $suffix),
  ('({0}, ''music_seen_{1}'', ''demo'', ''videos/music_seen_{1}.mp4'', JSON_ARRAY(''music''), ''APPROVED'', 0),' -f $userId, $suffix),
  ('({0}, ''music_cand_{1}'', ''demo'', ''videos/music_cand_{1}.mp4'', JSON_ARRAY(''music''), ''APPROVED'', 0);' -f $userId, $suffix),
  "",
  "-- LAST_INSERT_ID() returns the first inserted id for multi-row INSERT",
  "SET @v1 = LAST_INSERT_ID();",
  "SET @v2 = @v1 + 1;",
  "SET @v3 = @v1 + 2;",
  "SET @v4 = @v1 + 3;",
  "",
  "INSERT INTO video_stats(video_id, play_count, like_count, comment_count, favorite_count, hot_score) VALUES",
  "(@v1, 10, 1, 0, 0, 0),",
  "(@v2, 2, 0, 0, 0, 0),",
  "(@v3, 100, 10, 5, 1, 0),",
  "(@v4, 1, 0, 0, 0, 0);",
  "",
  "INSERT INTO user_actions(user_id, video_id, action_type, action_time) VALUES",
  ("({0}, @v1, 'FAVORITE', NOW(3));" -f $userId),
  "",
  "SELECT @v1 AS sports_seen, @v2 AS sports_cand, @v3 AS music_seen, @v4 AS music_cand;"
)

$sql = $sqlLines -join "`n"

docker exec -i $mysqlContainer mysql -uroot "-p$mysqlRootPass" $mysqlDb -e $sql | Out-Null

Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/admin/rank/hot/refresh" -Headers (BasicAuthHeader "admin" "AdminPass123") | Out-Null

"=== First recommendations (sports preference) ==="
$r1 = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/recommendations?page=1&pageSize=10" -Headers @{ Authorization = "Bearer $token" }
$r1.data.items | Select-Object id,title,tags,hotScore | Format-Table -AutoSize

$sql2Lines = @(
  ("SET @music_seen = (SELECT id FROM videos WHERE title='music_seen_{0}' LIMIT 1);" -f $suffix),
  "INSERT INTO user_actions(user_id, video_id, action_type, action_time) VALUES",
  ("({0}, @music_seen, 'FAVORITE', NOW(3))," -f $userId),
  ("({0}, @music_seen, 'FAVORITE', NOW(3));" -f $userId)
)

$sql2 = $sql2Lines -join "`n"
docker exec -i $mysqlContainer mysql -uroot "-p$mysqlRootPass" $mysqlDb -e $sql2 | Out-Null

"=== Second recommendations (after boosting music preference) ==="
$r2 = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/recommendations?page=1&pageSize=10" -Headers @{ Authorization = "Bearer $token" }
$r2.data.items | Select-Object id,title,tags,hotScore | Format-Table -AutoSize

