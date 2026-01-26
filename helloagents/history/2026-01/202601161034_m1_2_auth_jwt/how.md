# 技术设计: M1.2 用户注册/登录 + JWT 鉴权

## 技术方案

### 依赖与约束
- **DB:** MySQL（Flyway 迁移管理）
- **加密:** bcrypt（Spring Security Crypto）
- **鉴权:** JWT（HS256），Spring Security 最小化配置（不引入复杂权限模型）
- **验证码:** 最小实现=写入表/（可选 Redis）+ 控制台打印

### 表与迁移脚本编号
- `V4__email_verification_codes.sql`：新增邮箱验证码表
- （如需）`V5__...`：后续扩展

## 数据库设计（核心表使用口径）

### users（既有）
- `status`: 1=正常，0=冻结（本阶段新增约束：冻结禁止登录与写操作）
- 索引：用户名/手机号/邮箱唯一（已存在）

### email_verification_codes（新增）
- `email`、`code`、`purpose`（REGISTER）、`expires_at`、`used_at`
- 索引：按 `email+purpose+created_at` 查询最新未使用验证码

## 代码结构（新增/变更清单）

### Entity / Mapper
- `UserEntity` / `UserMapper`
- `EmailVerificationCodeEntity` / `EmailVerificationCodeMapper`

### Service
- `AuthService`：发送验证码、注册、登录
- `JwtService`：生成/校验 JWT
- `UserService`：查询当前用户、冻结校验（最小）

### Controller
- `AuthController`
  - `POST /api/auth/email-code`（最小实现）
  - `POST /api/auth/register`
  - `POST /api/auth/login`
- `UserController`
  - `GET /api/users/me`

### DTO
- `SendEmailCodeRequest`
- `RegisterRequest`
- `LoginRequest`
- `AuthResponse`（token + user）
- `MeResponse`

### 错误码（建议）
- `40000` 参数校验失败
- `40901` 用户名已存在
- `40902` 邮箱已存在
- `40903` 手机号已存在
- `40010` 邮箱验证码无效或已过期
- `40100` 未登录/Token 无效
- `40101` 账号或密码错误
- `40301` 账号已冻结

## 鉴权与冻结拦截策略
- JWT 解析成功后加载用户（DB），设置 SecurityContext
- 若用户已冻结：
  - 登录接口直接拒绝
  - 已登录用户对 `POST/PUT/PATCH/DELETE` 统一拒绝（为后续互动写接口预留）
  - `GET /api/users/me` 允许返回状态

## 测试点（至少覆盖）
1. 发送验证码→数据库写入（不回传验证码）
2. 注册成功→用户创建 + 返回 JWT
3. 重复注册→唯一性错误码（至少覆盖邮箱/用户名/手机号之一）
4. 登录成功→返回 JWT
5. 冻结用户→登录失败（40301），并对写请求拒绝

