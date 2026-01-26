#!/usr/bin/env bash
# 用途: 以测试配置启动后端并监听 18080 端口
# 用法: bash scripts/run_backend_test_18080.sh

set -euo pipefail

cd "$(dirname "$0")/../backend"

exec mvn -DskipTests test-compile spring-boot:run \
  -Dspring-boot.run.useTestClasspath=true \
  -Dspring-boot.run.profiles=test \
  -Dspring-boot.run.arguments="--server.port=18080 --spring.flyway.enabled=false --spring.sql.init.data-locations=classpath:seed-h2.sql"
