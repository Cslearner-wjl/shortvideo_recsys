CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64) NOT NULL UNIQUE,
  phone VARCHAR(32) NOT NULL UNIQUE,
  email VARCHAR(128) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE admin_users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64) NOT NULL UNIQUE,
  display_name VARCHAR(64) NULL,
  password_hash VARCHAR(100) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE email_verification_codes (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(128) NOT NULL,
  code VARCHAR(16) NOT NULL,
  purpose VARCHAR(32) NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  used_at TIMESTAMP NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_evc_email_purpose_created ON email_verification_codes(email, purpose, created_at);

-- 视频与互动（用于 test profile 的集成测试）
CREATE TABLE videos (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  uploader_user_id BIGINT NOT NULL,
  title VARCHAR(200) NOT NULL,
  description CLOB NULL,
  video_url VARCHAR(512) NOT NULL,
  cover_url VARCHAR(512) NULL,
  tags VARCHAR(2000) NULL,
  audit_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  is_hot TINYINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_videos_uploader_user_id FOREIGN KEY (uploader_user_id) REFERENCES users (id)
);

CREATE INDEX idx_videos_uploader ON videos(uploader_user_id);
CREATE INDEX idx_videos_created_at ON videos(created_at);
CREATE INDEX idx_videos_audit_status ON videos(audit_status);
CREATE INDEX idx_videos_is_hot ON videos(is_hot);

CREATE TABLE video_stats (
  video_id BIGINT NOT NULL PRIMARY KEY,
  play_count BIGINT NOT NULL DEFAULT 0,
  like_count BIGINT NOT NULL DEFAULT 0,
  comment_count BIGINT NOT NULL DEFAULT 0,
  favorite_count BIGINT NOT NULL DEFAULT 0,
  hot_score DOUBLE NOT NULL DEFAULT 0,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_video_stats_video_id FOREIGN KEY (video_id) REFERENCES videos (id) ON DELETE CASCADE
);

CREATE TABLE comments (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  video_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  content CLOB NOT NULL,
  like_count BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_comments_video_id FOREIGN KEY (video_id) REFERENCES videos (id) ON DELETE CASCADE,
  CONSTRAINT fk_comments_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_comments_video_time ON comments(video_id, created_at);
CREATE INDEX idx_comments_user_time ON comments(user_id, created_at);

CREATE TABLE comment_likes (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  comment_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_comment_likes_comment_id FOREIGN KEY (comment_id) REFERENCES comments (id) ON DELETE CASCADE,
  CONSTRAINT fk_comment_likes_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT uk_comment_likes_comment_user UNIQUE (comment_id, user_id)
);

CREATE INDEX idx_comment_likes_comment_time ON comment_likes(comment_id, created_at);
CREATE INDEX idx_comment_likes_user_time ON comment_likes(user_id, created_at);

CREATE TABLE video_likes (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  video_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_video_likes_video_id FOREIGN KEY (video_id) REFERENCES videos (id) ON DELETE CASCADE,
  CONSTRAINT fk_video_likes_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT uk_video_likes_video_user UNIQUE (video_id, user_id)
);

CREATE INDEX idx_video_likes_user_time ON video_likes(user_id, created_at);

CREATE TABLE video_favorites (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  video_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_video_favorites_video_id FOREIGN KEY (video_id) REFERENCES videos (id) ON DELETE CASCADE,
  CONSTRAINT fk_video_favorites_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT uk_video_favorites_video_user UNIQUE (video_id, user_id)
);

CREATE INDEX idx_video_favorites_user_time ON video_favorites(user_id, created_at);

CREATE TABLE user_actions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  video_id BIGINT NOT NULL,
  action_type VARCHAR(32) NOT NULL,
  action_time TIMESTAMP NOT NULL,
  duration_ms BIGINT NULL,
  is_completed TINYINT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_user_actions_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_user_actions_video_id FOREIGN KEY (video_id) REFERENCES videos (id) ON DELETE CASCADE
);

CREATE INDEX idx_user_actions_user_time ON user_actions(user_id, action_time);
CREATE INDEX idx_user_actions_video_time ON user_actions(video_id, action_time);
CREATE INDEX idx_user_actions_type_time ON user_actions(action_type, action_time);
