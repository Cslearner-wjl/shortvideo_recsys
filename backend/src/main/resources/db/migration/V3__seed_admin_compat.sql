-- V3: 更新默认管理员记录（兼容 MySQL 新版本语法提示）
--
-- 说明：
-- 1) V2 已发布后不可修改（Flyway checksum），因此用 V3 做增量修正。
-- 2) 将 display_name 改为 ASCII，避免在部分终端/客户端出现乱码。

INSERT INTO admin_users (username, display_name, password_hash, status, created_at, updated_at)
VALUES (
  'admin',
  'Admin',
  '$2b$12$OeXCDlYn6UGqGaxZRKMGae7MwCj4KyPA88eDDdaF0bs8KtQBPvhUe',
  1,
  CURRENT_TIMESTAMP(3),
  CURRENT_TIMESTAMP(3)
) AS new
ON DUPLICATE KEY UPDATE
  display_name = new.display_name,
  password_hash = new.password_hash,
  status = new.status,
  updated_at = new.updated_at;

