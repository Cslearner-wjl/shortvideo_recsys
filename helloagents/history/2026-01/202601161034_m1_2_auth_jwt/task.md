# 任务清单: M1.2 用户注册/登录 + JWT 鉴权

目录: `helloagents/plan/202601161034_m1_2_auth_jwt/`

---

## 1. 数据库迁移
- [√] 1.1 新增 Flyway 迁移 `backend/src/main/resources/db/migration/V4__email_verification_codes.sql`（验证码表 + 索引）

## 2. 后端实现（最小可用）
- [√] 2.1 增加 Spring Security 最小配置（放行 `/api/auth/**`、鉴权 `/api/users/me`）
- [√] 2.2 实现 JWT（HS256）：生成/校验、过滤器、当前用户注入
- [√] 2.3 实现验证码最小发送：写入表 + 控制台打印；有效期 5 分钟
- [√] 2.4 实现注册：手机号 11 位、邮箱格式、唯一性校验、验证码校验、bcrypt 存储
- [√] 2.5 实现登录：账号三选一 + 密码校验；失败提示“账号或密码错误”；冻结禁止登录
- [√] 2.6 实现 `GET /api/users/me`：需要 JWT
- [√] 2.7 冻结拦截：冻结用户禁止写操作（为互动写接口预留）

## 3. 文档
- [√] 3.1 新增/更新接口文档（auth/users + 错误码 + 示例）
- [√] 3.2 更新数据字典与知识库（`docs/数据字典草案.md`、`helloagents/wiki/api.md`、`helloagents/wiki/data.md`、`helloagents/wiki/modules/backend.md`）
- [√] 3.3 更新 `helloagents/CHANGELOG.md`

## 4. 测试
- [√] 4.1 增加集成测试：注册/登录/冻结（至少覆盖 3 条）
- [√] 4.2 执行测试并记录结果
  > 备注: `mvn test` 已通过（H2 test profile）；docker+MySQL 环境下启动日志显示 Flyway 迁移至 v4。

## 5. 安全检查
- [√] 5.1 确认无明文密钥提交；JWT secret 仅提供本地默认值且可被环境变量覆盖
