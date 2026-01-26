# 短视频分析推荐系统

> 本文档包含项目级别的核心信息；模块级细节见 `modules/`。

---

## 1. 项目概述

### 目标与背景

通过采集用户行为（播放、点赞、评论、收藏）与视频内容信息，实现短视频内容管理、热门榜单统计、个性化推荐与数据分析看板，提升内容分发效率与用户留存。

### 范围

- **范围内:** 注册登录、视频管理与展示、互动行为采集、热门榜单（基础统计）、推荐（分阶段：规则 → 协同过滤）、管理端能力、基础看板（按需落地）
- **范围外（暂不承诺）:** 深度学习推荐、复杂风控审核、生产级多租户/多地域部署

### 干系人

- **负责人:** 待定
- **用户角色:** 普通用户、管理员

---

## 2. 模块索引

> 状态含义：`规划中` / `开发中` / `稳定`

| 模块名称 | 职责 | 状态 | 文档 |
|---------|------|------|------|
| backend | REST API、业务逻辑、数据访问 | 开发中 | [modules/backend.md](modules/backend.md) |
| frontend-user | 用户端交互（刷视频/互动/榜单） | 开发中 | [modules/frontend-user.md](modules/frontend-user.md) |
| frontend-admin | 管理端（审核/管理/看板） | 稳定 | [modules/frontend-admin.md](modules/frontend-admin.md) |
| deploy | 本地依赖编排、环境初始化 | 开发中 | [modules/deploy.md](modules/deploy.md) |
| bigdata | 行为链路、实时/离线计算、ALS | 开发中 | [modules/bigdata.md](modules/bigdata.md) |
| docs | 外部需求/流程/约束文档 | 规划中 | [modules/docs.md](modules/docs.md) |
| frontend | 前端聚合目录（admin/user） | 开发中 | [modules/frontend.md](modules/frontend.md) |
| scripts | 演示与验收脚本（PowerShell） | 开发中 | [modules/scripts.md](modules/scripts.md) |

---

## 3. 快速链接

- [技术约定](../project.md)
- [架构设计](arch.md)
- [API 手册](api.md)
- [数据模型](data.md)
- [变更历史](../history/index.md)

