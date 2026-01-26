# 技术设计: 全量测试执行与测试文档

## 技术方案
### 核心技术
- JUnit / Spring Boot Test / MockMvc
- Node/Vite 相关测试脚本（按现有项目配置）
- JMeter
- Docker Compose（依赖环境）

### 实现要点
- 统一启动 `deploy/docker-compose.yml` 依赖。
- 分模块执行单元/集成/端到端测试。
- JMeter 压测覆盖核心接口并输出 TPS/P95/错误率。
- 每项测试均生成标准报告，归档在 `docs/tests/`。

## 测试与部署
- **测试:** 单元/集成/端到端/JMeter 压测
- **部署:** docker-compose 启动依赖后再执行测试
