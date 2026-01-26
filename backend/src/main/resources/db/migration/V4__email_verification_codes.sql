-- V4: 邮箱验证码（最小实现：写表 + 控制台打印）

CREATE TABLE email_verification_codes (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  email VARCHAR(128) NOT NULL,
  code VARCHAR(16) NOT NULL,
  purpose VARCHAR(32) NOT NULL,
  expires_at DATETIME(3) NOT NULL,
  used_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_evc_email_purpose_created (email, purpose, created_at),
  KEY idx_evc_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

