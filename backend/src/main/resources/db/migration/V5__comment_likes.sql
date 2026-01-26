ALTER TABLE comments
  ADD COLUMN like_count BIGINT UNSIGNED NOT NULL DEFAULT 0 AFTER content;

CREATE TABLE comment_likes (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  comment_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_comment_likes_comment_user (comment_id, user_id),
  KEY idx_comment_likes_comment_time (comment_id, created_at),
  KEY idx_comment_likes_user_time (user_id, created_at),
  CONSTRAINT fk_comment_likes_comment_id FOREIGN KEY (comment_id) REFERENCES comments (id) ON DELETE CASCADE,
  CONSTRAINT fk_comment_likes_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
