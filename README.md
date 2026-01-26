# shortvideo_recsys（M0 工程骨架）

本仓库当前完成 **P0/M0：工程骨架 + 本地一键启动**，用于后续 M1（业务闭环）/M2（看板）/M3（大数据链路）/M4（ALS）迭代。

---

## 快速入口
- 运行指南：`docs/运行指南.md`
- 脚本说明：`docs/脚本说明.md`
- 需求/流程/设计文档：`docs/`
- 知识库（SSOT）：`helloagents/`

---

## 目录结构

- `deploy/`：本地依赖一键启动（docker compose）
- `backend/`：Spring Boot 后端骨架（健康检查）
- `frontend/`
  - `frontend/user/`：用户端 Vue3(Vite) 骨架
  - `frontend/admin/`：管理端 Vue3(Vite) 骨架
- `docs/`：需求/流程/设计/运行说明文档
- `scripts/`：脚本与工具（含 `scripts/生成视频/`）
- `bigdata/`：大数据链路规划与预留目录
- `helloagents/`：知识库（SSOT）
