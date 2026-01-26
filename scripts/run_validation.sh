#!/bin/bash
# 用途: 执行本地验证流程脚本
# 用法: bash run_validation.sh

cd /home/wjl/workspace/shortvideo_recsys

echo "=== Step 1: Restart Backend ==="
pkill -f "spring-boot:run" || true
sleep 2
cd backend
nohup bash run_backend_18080.sh > nohup-validation.log 2>&1 &
BACKEND_PID=$!
echo "Backend started with PID: $BACKEND_PID"
cd ..

echo "Waiting for backend to start (30s)..."
sleep 30

echo ""
echo "=== Step 2: Check Backend Health ==="
curl -s http://localhost:18080/actuator/health | python3 -m json.tool || echo "Health check failed"

echo ""
echo "=== Step 3: Check BehaviorEventLogger Initialization ==="
grep "BehaviorEventLogger initialized" backend/nohup-validation.log | tail -1

echo ""
echo "=== Step 4: Trigger Events ==="
# Login
LOGIN_RESP=$(curl -s -X POST http://localhost:18080/api/auth/admin-login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin123!"}')
TOKEN=$(echo "$LOGIN_RESP" | python3 -c "import json,sys; print(json.load(sys.stdin).get('data',{}).get('token',''))" 2>/dev/null)

if [ -z "$TOKEN" ]; then
  echo "Failed to get token. Response:"
  echo "$LOGIN_RESP"
  exit 1
fi

echo "Token obtained: ${TOKEN:0:20}..."

# Trigger play event
echo "Triggering play event..."
PLAY_RESP=$(curl -s -X POST http://localhost:18080/api/videos/1/play \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"durationMs":5000,"isCompleted":true}')
echo "Play response: $PLAY_RESP"

# Trigger like event
echo "Triggering like event..."
LIKE_RESP=$(curl -s -X POST http://localhost:18080/api/videos/1/like \
  -H "Authorization: Bearer $TOKEN")
echo "Like response: $LIKE_RESP"

sleep 3

echo ""
echo "=== Step 5: Check Behavior Log File ==="
if [ -f backend/logs/behavior-events.log ]; then
  echo "✓ Log file created!"
  echo "Content:"
  cat backend/logs/behavior-events.log
else
  echo "✗ Log file NOT created"
  echo "Checking logs directory:"
  ls -la backend/logs/
fi

echo ""
echo "=== Step 6: Check Backend Logs for Behavior Events ==="
grep -i "appended behavior event\|behavior.*failed" backend/nohup-validation.log | tail -10

echo ""
echo "=== Step 7: Check Flume Status ==="
docker compose ps flume

echo ""
echo "=== Step 8: Check Kafka Topic ==="
docker exec shortvideo-recsys-kafka kafka-topics --bootstrap-server localhost:9092 --list | grep behavior || echo "Topic not found"

echo ""
echo "=== Validation Complete ==="
