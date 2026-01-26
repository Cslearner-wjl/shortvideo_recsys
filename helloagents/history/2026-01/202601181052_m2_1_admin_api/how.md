# 技术设计: M2.1 管理后台接口（用户管理/看板）

## 技术方案
### 核心技术
- Spring Boot + MyBatisPlus（现有技术栈）
- MySQL 聚合查询（不使用 Spark）
- Basic Auth + `ROLE_ADMIN` 权限链路

### 实现要点
- 新增 `AdminUserController` 与 `AdminAnalyticsController`，统一挂载 `/api/admin/**`
- 用户查询使用 `QueryWrapper` + `Page` 进行分页与关键字筛选
- 统计接口使用 Mapper 自定义 SQL 聚合（时间区间、TopN）
- 参数校验:
  - `page` 默认 1；`size` 默认 20，最大 100
  - `from/to` 必填且 `from <= to`
  - `n` 默认 10，最大 100
  - `status` 仅允许 0/1

## API设计

### GET /api/admin/users
- **权限:** `ROLE_ADMIN`（Basic Auth）
- **查询条件:** `keyword` 同时匹配 `username`/`phone`/`email`（精确或模糊）
- **分页标准:** `page`/`size`，参考 `VideoService.pageApproved` 的安全边界
- **响应:** `PageResponse<AdminUserDto>`

### PATCH /api/admin/users/{id}/status
- **权限:** `ROLE_ADMIN`（Basic Auth）
- **请求:** `status`（0 冻结 / 1 解封）
- **响应:** `ApiResponse<Void>`

### GET /api/admin/analytics/daily-play
- **权限:** `ROLE_ADMIN`
- **请求:** `from`/`to`（日期）
- **响应:** `List<DailyPlayDto>`

### GET /api/admin/analytics/user-growth
- **权限:** `ROLE_ADMIN`
- **请求:** `from`/`to`（日期）
- **响应:** `List<UserGrowthDto>`

### GET /api/admin/analytics/hot-topn
- **权限:** `ROLE_ADMIN`
- **请求:** `n`（TopN）
- **响应:** `List<HotTopnDto>`

### GET /api/admin/analytics/active-users
- **权限:** `ROLE_ADMIN`
- **请求:** `from`/`to`（日期）
- **响应:** `List<ActiveUserDto>`

## 数据模型

### 每日播放趋势
```sql
SELECT DATE(action_time) AS day, COUNT(*) AS play_count
FROM user_actions
WHERE action_type = 'PLAY'
  AND action_time >= #{fromStart}
  AND action_time < #{toEnd}
GROUP BY DATE(action_time)
ORDER BY day;
```

### 用户增长
```sql
SELECT DATE(created_at) AS day, COUNT(*) AS new_user_count
FROM users
WHERE created_at >= #{fromStart}
  AND created_at < #{toEnd}
GROUP BY DATE(created_at)
ORDER BY day;
```

### 活跃用户
```sql
SELECT DATE(action_time) AS day, COUNT(DISTINCT user_id) AS active_user_count
FROM user_actions
WHERE action_time >= #{fromStart}
  AND action_time < #{toEnd}
GROUP BY DATE(action_time)
ORDER BY day;
```

### 热门 TopN
```sql
SELECT v.id, v.title, vs.play_count, vs.like_count, vs.comment_count, vs.favorite_count, vs.hot_score
FROM video_stats vs
JOIN videos v ON v.id = vs.video_id
ORDER BY vs.hot_score DESC, vs.play_count DESC
LIMIT #{limit};
```

## 安全与性能
- **安全:** `/api/admin/**` 走 Basic Auth，确保 `ROLE_ADMIN`；参数校验失败直接返回 `BAD_REQUEST`
- **性能:** 控制分页/TopN上限，时间区间查询依赖现有索引字段

## 测试与部署
- **测试:**
  - 用户查询与冻结/解封接口集成测试（含 Basic Auth）
  - 看板统计接口集成测试（含时间范围与 TopN）
  - 测试库补齐 `admin_users` 表结构
- **部署:** 无额外部署变更

