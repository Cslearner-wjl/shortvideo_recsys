-- V2: 初始化管理员账号（仅用于本地开发/测试环境）
--
-- 明文示例: Admin123!
-- bcrypt 哈希生成方式（WSL/Ubuntu 示例）：
--   python3 - <<'PY'
--   import crypt
--   print(crypt.crypt("Admin123!", crypt.mksalt(crypt.METHOD_BLOWFISH)))
--   PY

INSERT INTO admin_users (username, display_name, password_hash, status, created_at, updated_at)
VALUES (
  'admin',
  '管理员',
  '$2b$12$OeXCDlYn6UGqGaxZRKMGae7MwCj4KyPA88eDDdaF0bs8KtQBPvhUe',
  1,
  CURRENT_TIMESTAMP(3),
  CURRENT_TIMESTAMP(3)
)
ON DUPLICATE KEY UPDATE
  display_name = VALUES(display_name),
  password_hash = VALUES(password_hash),
  status = VALUES(status),
  updated_at = VALUES(updated_at);
