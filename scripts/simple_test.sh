#!/bin/bash
# 用途: 本地快速注册登录并触发播放/点赞事件，验证行为日志
# 用法: bash simple_test.sh

cd /home/wjl/workspace/shortvideo_recsys

echo "=== Step 1: Register a test user ==="
REG_RESP=$(curl -s -X POST http://localhost:18080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser999","password":"Test123456","email":"test@example.com","emailCode":"000000"}')
echo "Register response: $REG_RESP"

echo ""
echo "=== Step 2: Login with testuser999/Test123456 ==="
LOGIN_RESP=$(curl -s -X POST http://localhost:18080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"account":"testuser999","password":"Test123456"}')
echo "Login response: $LOGIN_RESP"

TOKEN=$(echo "$LOGIN_RESP" | python3 -c "import json,sys; d=json.load(sys.stdin); print(d.get('data',{}).get('token',''))" 2>/dev/null || echo "")

if [ -z "$TOKEN" ]; then
  echo "✗ Login failed, trying existing user..."
  # Maybe user already exists, just login
  sleep 1
fi

if [ -z "$TOKEN" ]; then
  echo "Cannot get token, exiting"
  exit 1
fi

echo "✓ Token: ${TOKEN:0:30}..."

echo ""
echo "=== Step 3: Trigger play event ==="
PLAY_RESP=$(curl -s -X POST http://localhost:18080/api/videos/1/play \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"durationMs":5000,"isCompleted":true}')
echo "Response: $PLAY_RESP"

sleep 1

echo ""
echo "=== Step 4: Trigger like event ==="
LIKE_RESP=$(curl -s -X POST http://localhost:18080/api/videos/1/like \
  -H "Authorization: Bearer $TOKEN")
echo "Response: $LIKE_RESP"

sleep 2

echo ""
echo "=== Step 5: Check log file ==="
if [ -f backend/logs/behavior-events.log ]; then
  echo "✓ Log file exists!"
  cat backend/logs/behavior-events.log
else
  echo "✗ Log file not found"
  echo "Directory contents:"
  ls -la backend/logs/ 2>/dev/null || echo "logs dir missing"
fi

echo ""
echo "=== Step 6: Check backend logs for behavior events ==="
grep -i "Appended behavior event" backend/nohup-validation.log | tail -10 || echo "No append logs found"

echo ""
echo "===完成 ==="
