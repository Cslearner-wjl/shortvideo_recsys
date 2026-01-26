#!/bin/bash
# 用途: 验证行为日志写入与落盘情况
# 用法: bash test_behavior_logging.sh

cd /home/wjl/workspace/shortvideo_recsys

echo "=== Creating Test User ===" 
# 使用 bcrypt 哈希 "Test123456"
docker exec shortvideo-recsys-mysql mysql -u root -prootpass shortvideo_recsys -e "
INSERT IGNORE INTO users (username, password_hash, status, created_at, updated_at)
VALUES ('testuser', '\$2b\$12\$N6SdVNqgfE9ZQlgVzTJqF.2Vj2tZ5n2F5zt3kJH2Kf2W3sYzFx3Fm', 1, NOW(), NOW());
"

echo ""
echo "=== Testing Login with testuser/Test123456 ==="
LOGIN_RESP=$(curl -s -X POST http://localhost:18080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"account":"testuser","password":"Test123456"}')
echo "Login response: $LOGIN_RESP"

TOKEN=$(echo "$LOGIN_RESP" | python3 -c "import json,sys; print(json.load(sys.stdin).get('data',{}).get('token',''))" 2>/dev/null)

if [ -z "$TOKEN" ]; then
  echo "✗ Login failed"
  exit 1
fi

echo "✓ Token obtained: ${TOKEN:0:30}..."

echo ""
echo "=== Triggering Behavior Events ==="
# Play event
PLAY_RESP=$(curl -s -X POST http://localhost:18080/api/videos/1/play \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"durationMs":5000,"isCompleted":true}')
echo "Play response: $PLAY_RESP"

sleep 1

# Like event
LIKE_RESP=$(curl -s -X POST http://localhost:18080/api/videos/1/like \
  -H "Authorization: Bearer $TOKEN")
echo "Like response: $LIKE_RESP"

sleep 2

echo ""
echo "=== Checking Behavior Log File ==="
if [ -f backend/logs/behavior-events.log ]; then
  echo "✓ Log file created!"
  echo "Content:"
  cat backend/logs/behavior-events.log
else
  echo "✗ Log file NOT found"
  ls -la backend/logs/ 2>/dev/null || echo "logs directory not found"
fi

echo ""
echo "=== Checking Backend Logs ==="
grep -i "appended behavior event" backend/nohup-validation.log | tail -5

echo ""
echo "=== Flume Status ==="
docker compose ps flume | tail -2

echo ""
echo "=== Done ==="
