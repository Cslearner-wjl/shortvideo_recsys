# frontend

## 管理端（frontend/admin）

### 运行命令
```
cd frontend/admin
npm install
npm run dev
```

### 联调说明
- 默认通过 Vite proxy 转发 `/api`，可用 `VITE_PROXY_TARGET` 指向后端（默认 `http://localhost:18080`）。
- 管理端接口使用 **Basic Auth**，账号来自后端管理员表（`admin_user`）。
- 登录页会通过 `/api/admin/users` 验证 Basic Auth 成功与否。

### 已落地页面
- 管理员登录页
- 用户管理页（分页/搜索/冻结解封）
- 视频管理页（列表/审核/设热门/删除/上传）
- 数据看板页（播放趋势、用户增长、TopN 热门、活跃度）

### 注意事项
- 视频列表复用 `/api/videos/page`，仅返回审核通过的视频；待审列表需后端补充接口。
