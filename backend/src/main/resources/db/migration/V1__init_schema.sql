-- V1: 初始化核心表结构（MySQL 8.x）

CREATE TABLE users (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL,
  phone VARCHAR(32) NULL,
  email VARCHAR(128) NULL,
  password_hash VARCHAR(100) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_username (username),
  UNIQUE KEY uk_users_phone (phone),
  UNIQUE KEY uk_users_email (email),
  KEY idx_users_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE admin_users (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL,
  display_name VARCHAR(64) NULL,
  password_hash VARCHAR(100) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_admin_users_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE videos (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  uploader_user_id BIGINT UNSIGNED NOT NULL,
  title VARCHAR(200) NOT NULL,
  description TEXT NULL,
  video_url VARCHAR(512) NOT NULL,
  cover_url VARCHAR(512) NULL,
  tags JSON NULL,
  audit_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  is_hot TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_videos_uploader (uploader_user_id),
  KEY idx_videos_created_at (created_at),
  KEY idx_videos_audit_status (audit_status),
  KEY idx_videos_is_hot (is_hot),
  CONSTRAINT fk_videos_uploader_user_id FOREIGN KEY (uploader_user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE video_stats (
  video_id BIGINT UNSIGNED NOT NULL,
  play_count BIGINT UNSIGNED NOT NULL DEFAULT 0,
  like_count BIGINT UNSIGNED NOT NULL DEFAULT 0,
  comment_count BIGINT UNSIGNED NOT NULL DEFAULT 0,
  favorite_count BIGINT UNSIGNED NOT NULL DEFAULT 0,
  hot_score DOUBLE NOT NULL DEFAULT 0,
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (video_id),
  CONSTRAINT fk_video_stats_video_id FOREIGN KEY (video_id) REFERENCES videos (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE comments (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  video_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  content TEXT NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_comments_video_time (video_id, created_at),
  KEY idx_comments_user_time (user_id, created_at),
  CONSTRAINT fk_comments_video_id FOREIGN KEY (video_id) REFERENCES videos (id) ON DELETE CASCADE,
  CONSTRAINT fk_comments_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE video_likes (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  video_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_video_likes_video_user (video_id, user_id),
  KEY idx_video_likes_user_time (user_id, created_at),
  CONSTRAINT fk_video_likes_video_id FOREIGN KEY (video_id) REFERENCES videos (id) ON DELETE CASCADE,
  CONSTRAINT fk_video_likes_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE video_favorites (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  video_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_video_favorites_video_user (video_id, user_id),
  KEY idx_video_favorites_user_time (user_id, created_at),
  CONSTRAINT fk_video_favorites_video_id FOREIGN KEY (video_id) REFERENCES videos (id) ON DELETE CASCADE,
  CONSTRAINT fk_video_favorites_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE user_actions (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  video_id BIGINT UNSIGNED NOT NULL,
  action_type VARCHAR(32) NOT NULL,
  action_time DATETIME(3) NOT NULL,
  duration_ms BIGINT UNSIGNED NULL,
  is_completed TINYINT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_user_actions_user_time (user_id, action_time),
  KEY idx_user_actions_video_time (video_id, action_time),
  KEY idx_user_actions_type_time (action_type, action_time),
  CONSTRAINT fk_user_actions_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_user_actions_video_id FOREIGN KEY (video_id) REFERENCES videos (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE daily_metrics (
  day DATE NOT NULL,
  play_count BIGINT UNSIGNED NOT NULL DEFAULT 0,
  like_count BIGINT UNSIGNED NOT NULL DEFAULT 0,
  comment_count BIGINT UNSIGNED NOT NULL DEFAULT 0,
  favorite_count BIGINT UNSIGNED NOT NULL DEFAULT 0,
  active_user_count BIGINT UNSIGNED NOT NULL DEFAULT 0,
  new_user_count BIGINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (day)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

