# 测试报告 - 后端集成测试

## 1. 基本信息

- 测试类型: 集成测试
- 模块: backend
- 执行日期: 2026-01-22
- 执行人: HelloAGENTS
- 版本: Unreleased

## 2. 测试环境

- JDK 17
- Maven
- Docker Compose: MySQL / Redis / MinIO
- Spring Boot Test（docker profile）

## 3. 测试范围

- *IntegrationTest
- BackendApplicationTests

## 4. 执行步骤

- 依赖启动: `docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d`
- `cd backend`
- `RUN_MINIO_IT=true mvn -Dtest=*IntegrationTest,BackendApplicationTests test`

## 5. 测试结果

- 结论: 通过
- 通过项: 集成测试全量通过
- 失败项: 0

## 6. 风险与问题

- 无

## 7. 附件

- Maven Surefire 报告目录: `backend/target/surefire-reports/`
