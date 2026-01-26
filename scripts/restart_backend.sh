#!/bin/bash
# 用途: 重启后端服务进程
# 用法: bash restart_backend.sh

cd backend
echo 'Stopping backend...'
pkill -f 'spring-boot:run' 2>/dev/null ; true
sleep 2
echo 'Starting backend on 18080...'
nohup bash run_backend_18080.sh > nohup-18080-new.log 2>&1 &
sleep 5
echo 'Waiting for backend to start...'
for i in {1..20}; do
  if curl -s http://localhost:18080/actuator/health | grep -q UP; then
    echo 'Backend is UP!'
    exit 0
  fi
  sleep 3
done
echo 'Backend start timeout'
