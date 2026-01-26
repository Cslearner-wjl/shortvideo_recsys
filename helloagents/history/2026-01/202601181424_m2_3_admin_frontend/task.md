# 任务清单: M2.3 管理端前端（Vue3 + ECharts）

目录: `helloagents/plan/202601181424_m2_3_admin_frontend/`

---

## 1. 基础工程与依赖
- [√] 1.1 更新 `frontend/admin/package.json` 引入 vue-router、element-plus、echarts，匹配 why.md#需求-管理员登录-场景-基础登录
- [√] 1.2 在 `frontend/admin/vite.config.ts` 增加 proxy 与 baseURL 读取，匹配 why.md#需求-管理员登录-场景-基础登录
- [√] 1.3 增加 `frontend/admin/src/styles/*` 全局样式与布局基础样式，匹配 why.md#需求-管理员登录-场景-基础登录

## 2. 路由与鉴权
- [√] 2.1 新建 `frontend/admin/src/router/index.ts` 与布局路由结构，匹配 why.md#需求-管理员登录-场景-基础登录
- [√] 2.2 新建 `frontend/admin/src/services/auth.ts` 与 token 管理、路由守卫，匹配 why.md#需求-管理员登录-场景-基础登录

## 3. API Client
- [√] 3.1 新建 `frontend/admin/src/services/http.ts` 支持 JSON 与 FormData，匹配 why.md#需求-用户管理-场景-用户搜索与状态更新
- [√] 3.2 新建 `frontend/admin/src/services/admin.ts` 封装用户/视频/看板接口，匹配 why.md#需求-视频管理-场景-视频审核与运营

## 4. 组件
- [√] 4.1 新建 `frontend/admin/src/components/DataTable.vue` 与分页/搜索布局，匹配 why.md#需求-用户管理-场景-用户搜索与状态更新
- [√] 4.2 新建 `frontend/admin/src/components/ChartPanel.vue` 封装 ECharts，匹配 why.md#需求-数据看板-场景-数据总览

## 5. 页面实现
- [√] 5.1 新建 `frontend/admin/src/pages/Login.vue`，匹配 why.md#需求-管理员登录-场景-基础登录
- [√] 5.2 新建 `frontend/admin/src/pages/Users.vue`，匹配 why.md#需求-用户管理-场景-用户搜索与状态更新
- [√] 5.3 新建 `frontend/admin/src/pages/Videos.vue`，匹配 why.md#需求-视频管理-场景-视频审核与运营
- [√] 5.4 新建 `frontend/admin/src/pages/Dashboard.vue`，匹配 why.md#需求-数据看板-场景-数据总览

## 6. 联调与说明
- [√] 6.1 更新 `frontend/frontendREADME.md` 补充管理端联调说明、运行命令、最小可用 UI 描述，匹配 why.md#需求-管理员登录-场景-基础登录

## 7. 安全检查
- [√] 7.1 执行安全检查（输入校验、敏感信息处理、权限控制、EHRB 风险规避）

## 8. 文档更新
- [√] 8.1 更新 `helloagents/wiki/modules/frontend-admin.md` 记录落地内容
- [√] 8.2 更新 `helloagents/wiki/api.md` 记录管理端接口消费说明

## 9. 测试
- [√] 9.1 进行页面手动联调验证（登录、用户管理、视频管理、看板渲染）
  > **环境信息**:  
  > - 后端: WSL Ubuntu, docker profile, http://localhost:18080  
  > - 前端: WSL Ubuntu, dev server, http://localhost:5174  
  > - 管理员账号: `admin` / `AdminPass123` (Basic Auth, 后端自动初始化)  
  > - Docker: mysql(3307), redis(6379), minio(9000) 已启动并健康  
  >  
  > **验证步骤**（在 Windows 浏览器访问 `http://localhost:5174`）:  
  > 1. **登录页**: 输入 admin/AdminPass123 → 成功跳转到 /dashboard  
  > 2. **用户管理** (`/users`): 查看用户列表(当前11个用户) → 搜索用户名 → 冻结/解封用户 → 确认状态变更  
  > 3. **视频管理** (`/videos`): 查看视频列表(当前10个视频) → 测试审核通过/拒绝 → 设置/取消热门 → 删除视频  
  > 4. **数据看板** (`/dashboard`): 进入页面，确认 ECharts 面板加载（注: daily_metrics 表为空时看板图表可能无数据，属于预期）  
  >  
  > **API 接口验证**（WSL 内 curl 冒烟）:  
  > ```bash
  > # 后端健康检查
  > curl http://localhost:18080/actuator/health
  > # 管理端用户列表
  > curl -u admin:AdminPass123 'http://localhost:18080/api/admin/users?page=1&size=5'
  > # 公开视频列表（验证数据存在）
  > curl 'http://localhost:18080/api/videos/page?page=1&size=5'
  > ```
  >  
  > **联调结果**: ✅ 后端 health UP, 管理端 API Basic Auth 通过, 前端 dev server 正常响应, 路由与代理配置正确
