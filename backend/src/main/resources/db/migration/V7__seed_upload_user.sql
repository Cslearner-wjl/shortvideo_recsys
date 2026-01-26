-- V4: 预置批量导入用户（仅用于本地开发/测试环境）
-- 明文示例: password
-- bcrypt 哈希生成方式（WSL/Ubuntu 示例）：
--   python3 - <<'PY'
--   import crypt
--   print(crypt.crypt("password", crypt.mksalt(crypt.METHOD_BLOWFISH)))
--   PY

INSERT INTO users (username, phone, email, password_hash, status, created_at, updated_at)
VALUES (
  'uploaduser',
  '13900000000',
  'uploaduser@example.com',
  '$2b$12$p8sBXLrkL7s/l2PIyh224OZjh7VybxHaSjOcpoOrB8mRuPGTPI9La',
  1,
  CURRENT_TIMESTAMP(3),
  CURRENT_TIMESTAMP(3)
)
ON DUPLICATE KEY UPDATE
  password_hash = VALUES(password_hash),
  status = VALUES(status),
  updated_at = VALUES(updated_at);
