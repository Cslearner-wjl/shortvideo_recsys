# shortvideo_recsys（短视频分析推荐系统）

本仓库为一个 **端到端短视频分析与推荐系统** 的单仓（monorepo）实现：后端业务 + 前端用户端/管理端 + 行为日志链路（Flume/Kafka/HDFS）+ 实时统计（Spark Streaming）+ 离线推荐（Spark MLlib ALS）。

> 唯一真实来源（SSOT）：`helloagents/wiki/`（项目概览、架构、API、数据模型、模块文档与历史记录）。

---

## 当前完成情况（以代码为准）

- **后端（Spring Boot）**：注册登录（邮箱验证码）/ JWT、管理员 Basic Auth、视频上传-审核-发布（MinIO）、点赞/收藏/评论与计数聚合、热门榜单（Redis ZSET）、推荐（Redis ALS 优先 + 规则回退）、行为事件落盘（供 Flume 采集）。
- **前端**：
  - `frontend/user`：刷视频/榜单/推荐、互动、个人中心、详情页（浅色主题 + 中文文案）。
  - `frontend/admin`：管理员登录、用户管理、视频管理/审核、运营看板（ECharts）。
- **大数据链路**：
  - M3：行为日志文件 → Flume → Kafka（可选落 HDFS），Spark Streaming 实时统计写 Redis（热榜/指标）。
  - M4：ALS 离线训练写 Redis（`rec:user:{userId}`），后端推荐接口优先读取。
- **测试与报告**：
  - 后端：Spring Boot Test / MockMvc（`test` profile 使用 H2）。
  - 前端：Vitest + Playwright（报告落 `docs/tests/frontend-*`，目录内生成物默认不纳入 Git）。
  - 压测：JMeter 脚本 + 报告（Markdown 汇总在 `docs/tests/jmeter/*.md`；JTL/HTML 报告目录为本地生成物，默认不纳入 Git）。

---

## 快速入口

- 运行指南：`docs/运行指南.md`
- 脚本说明：`docs/脚本说明.md`
- 系统设计（文档版）：`docs/plans/2026-01-22-shortvideo-system-design.md`
- 知识库（SSOT）：`helloagents/wiki/overview.md`

---

## 一键启动（本地 docker 联调推荐）

### 1) 启动依赖（MySQL/Redis/MinIO/Kafka/HDFS/Flume）

```bash
cp -f deploy/.env.example deploy/.env
docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d
```

默认端口（可在 `deploy/.env` 调整）：
- MySQL：`3307`（映射到容器 `3306`）
- Redis：`6379`
- MinIO：`9000/9001`
- Kafka：`9093`（宿主机访问），容器内为 `kafka:9092`
- HDFS：`9870/8020/9864`

### 2) 启动后端（推荐：docker profile + 18080）

```bash
bash backend/run_backend_18080.sh
```

### 3) 启动前端

用户端：
```bash
cd frontend/user && npm install && npm run dev
```

管理端：
```bash
cd frontend/admin && npm install && npm run dev
```

---

## 常用脚本（验收/联调）

- 实时热榜（M3.2）：`bash scripts/run_m3_2_streaming.sh`
- ALS 离线推荐（M4）：`bash scripts/run_m4_als_batch.sh`
- 压测：
  - 混合场景：`bash scripts/jmeter_stage1_mix.sh`
  - 摸高：`bash scripts/jmeter_stage2_peak.sh`
  - 浸泡：`bash scripts/jmeter_stage3_soak.sh`

---

## 目录结构

- `deploy/`：本地依赖一键启动（docker compose）、示例环境变量
- `backend/`：后端服务（Java 17 / Spring Boot）
- `frontend/`：前端工程
  - `frontend/user/`：用户端（Vue 3 / Vite）
  - `frontend/admin/`：管理端（Vue 3 / Vite）
- `bigdata/`：行为链路与实时/离线计算（Streaming/ALS）
- `scripts/`：联调/验收/压测脚本
- `docs/`：外部文档与测试报告入口（本地生成物会被忽略）
- `helloagents/`：知识库（SSOT：`project.md`、`wiki/*`、`history/*`、`CHANGELOG.md`）
