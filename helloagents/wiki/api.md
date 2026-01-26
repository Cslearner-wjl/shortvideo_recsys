# API 手册

> 以执行事实为准：本文用于汇总当前已落地接口（M1.2/M1.3/M1.4/M1.5），并保留少量规划接口作为 TODO。若与代码不一致，以代码为准并更新本文。

---

## 概述

- **风格:** REST（JSON）
- **统一响应:** `ApiResponse(code, message, data)`（成功 `code=0`）
- **运行约束:** 业务接口（除 `/api/health`）以 `docker` / `test` profile 启用为准；默认 `local` profile 仅用于最小启动/健康检查，不保证业务接口可用。

---

## 认证方式

- **用户端:** JWT（`Authorization: Bearer <token>`）
- **管理端:** Basic Auth（`/api/admin/**`），管理端前端直接使用 Basic Auth，无独立登录接口
- **匿名访问:** 仅开放只读接口（GET `/api/videos/**`、GET `/api/rank/**`、GET `/api/health`）

---

## 接口列表（已落地为主）

### 健康检查

- ✅ `GET /api/health`

### 用户与鉴权（M1.2）

- ✅ `POST /api/auth/email-code`：发送注册邮箱验证码（最小实现：写表 + 控制台打印）
- ✅ `POST /api/auth/register`：注册（用户名/手机/邮箱/密码/邮箱验证码）
- ✅ `POST /api/auth/login`：登录（用户名/邮箱/手机号三选一 + 密码）
- ✅ `GET /api/users/me`：当前用户信息（需 JWT）
- ✅ `PUT /api/users/me`：更新个人资料（昵称/手机/邮箱/头像/简介）
- ✅ `POST /api/users/me/password`：修改当前用户密码

### 视频（M1.3）

- ✅ `POST /api/admin/videos`：管理端上传视频（multipart，对接 MinIO，写入 `videos` 待审）
- ✅ `PATCH /api/admin/videos/{id}/audit`：审核（`APPROVED`/`REJECTED`）
- ✅ `PATCH /api/admin/videos/{id}/hot`：设置热门
- ✅ `DELETE /api/admin/videos/{id}`：删除（DB + MinIO）
- ✅ `GET /api/videos/page`：视频分页查询（`sort=time|hot`，仅展示审核通过，返回统计字段与作者信息；携带 JWT 时包含 `liked`/`favorited`）
- ✅ `GET /api/videos/{id}`：视频详情（仅展示审核通过，返回统计字段与作者信息；携带 JWT 时包含 `liked`/`favorited`）
> 说明: 批量导入脚本通过管理端 Basic Auth 调用上传/审核接口，`uploaderUserId` 建议使用预置的 `uploaduser`。

### 互动与行为采集（M1.4）

- ✅ `POST /api/videos/{id}/play`：播放上报（需 JWT，可选字段 `durationMs`/`isCompleted`）
- ✅ `POST /api/videos/{id}/like`：点赞（需 JWT）
- ✅ `DELETE /api/videos/{id}/like`：取消点赞（需 JWT）
- ✅ `POST /api/videos/{id}/favorite`：收藏（需 JWT）
- ✅ `DELETE /api/videos/{id}/favorite`：取消收藏（需 JWT）
- ✅ `POST /api/videos/{id}/comments`：评论（需 JWT，最简结构）
- ✅ `GET /api/videos/{id}/comments`：评论列表（分页/排序=时间；可携带 JWT 获取 liked 状态，包含用户信息）
- ✅ `POST /api/comments/{id}/likes`：评论点赞（需 JWT）
- ✅ `DELETE /api/comments/{id}/likes`：取消评论点赞（需 JWT）

### 榜单与推荐（M1.5/规划）

- ✅ `GET /api/rank/hot`：热门榜单（分页返回；M3.2 可由 Streaming 维护 Redis 实时统计）
- ✅ `GET /api/recommendations`：推荐列表（需 JWT，返回 `liked`/`favorited`）
  - 默认：规则推荐（阶段1）
  - M4（可选）：若开启 `RECO_ALS_ENABLED=true` 且 Redis 存在 `rec:user:{userId}`，则优先按 ALS 顺序返回；未命中回退规则推荐

### 管理端（M2.3 已落地）

- ✅ `POST /api/admin/rank/hot/refresh`：手动刷新热门榜单（用于验收/运维，需 Basic Auth）
- ✅ `GET /api/admin/users`：用户列表（分页与关键字查询，参数 `page`/`size`/`keyword`）
- ✅ `PATCH /api/admin/users/{id}/status`：冻结/解封
- ✅ `GET /api/admin/admins`：管理员账号列表（分页/关键字）
- ✅ `POST /api/admin/admins`：新增管理员账号
- ✅ `PUT /api/admin/admins/{id}`：更新管理员账号（可修改密码）
- ✅ `DELETE /api/admin/admins/{id}`：删除管理员账号
- ✅ `POST /api/admin/admins/me/password`：管理员修改自己的密码
- ✅ `GET /api/admin/analytics/daily-play`：每日播放趋势（`from`/`to`）
- ✅ `GET /api/admin/analytics/user-growth`：用户增长（`from`/`to`）
- ✅ `GET /api/admin/analytics/hot-topn`：热门 TopN（`n` 可选）
- ✅ `GET /api/admin/analytics/active-users`：活跃用户（`from`/`to`）
- ✅ `GET /api/admin/analytics/video-publish`：视频发布量趋势（`from`/`to`）
- ✅ `GET /api/admin/videos/{id}/comments`：评论列表（管理端字段）
> 说明: 管理端视频列表当前复用 `GET /api/videos/page`，仅返回审核通过的视频。

