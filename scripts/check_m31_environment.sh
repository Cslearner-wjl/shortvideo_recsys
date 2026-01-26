#!/bin/bash
# 用途: 检查 M3.1 环境依赖与配置
# 用法: bash check_m31_environment.sh

cd /home/wjl/workspace/shortvideo_recsys

echo "====== M3.1 行为事件验收 ======" 
echo ""
echo "检查环境配置："
echo "-----------------------"
echo "BehaviorEventLogger 初始化:"
grep "BehaviorEventLogger initialized" backend/nohup-validation.log | tail -1

echo ""
echo "Flume 状态:"
docker compose ps flume 2>/dev/null | grep -v "NAME"

echo ""
echo "Kafka 状态:"
docker compose ps kafka 2>/dev/null | grep -v "NAME"

echo ""
echo "后端健康检查:"
curl -s http://localhost:18080/actuator/health | python3 -m json.tool 2>/dev/null || echo "后端未响应"

echo ""
echo "=======================检查完成 ====================="
echo ""
echo "请手动测试行为事件:"
echo "1. 访问前端页面触发播放、点赞等操作"
echo "2. 或使用 curl 命令测试（需要先登录获取token）:"
echo "   TOKEN=\$(curl -s http://localhost:18080/api/auth/login -H 'Content-Type: application/json' -d '{\"account\":\"你的账号\",\"password\":\"你的密码\"}' | python3 -c 'import json,sys; print(json.load(sys.stdin)[\"data\"][\"token\"])')"
echo "   curl -X POST http://localhost:18080/api/videos/1/play -H 'Authorization: Bearer \$TOKEN' -H 'Content-Type: application/json' -d '{\"durationMs\":5000,\"isCompleted\":true}'"
echo ""
echo "3. 然后检查日志文件:"
echo "   cat backend/logs/behavior-events.log"
echo ""
echo "4. 检查 Kafka 消息:"
echo "   docker exec shortvideo-recsys-kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic behavior-events --from-beginning --max-messages 5"
