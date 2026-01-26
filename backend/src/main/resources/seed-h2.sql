-- 仅用于“本地冒烟联调”启动：给 H2 插入少量演示数据（不影响 mvn test 默认行为）

INSERT INTO users(id, username, phone, email, password_hash, status, created_at, updated_at)
VALUES (1, 'demo_uploader', '13000000001', 'demo_uploader@example.com', 'hash', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO videos(id, uploader_user_id, title, description, video_url, cover_url, tags, audit_status, is_hot, created_at, updated_at)
VALUES (
  1,
  1,
  'Demo Video',
  '用于用户端冒烟验证的视频（H2 seed）',
  'https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4',
  NULL,
  '["demo","seed"]',
  'APPROVED',
  1,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
);

INSERT INTO video_stats(video_id, play_count, like_count, comment_count, favorite_count, hot_score, updated_at)
VALUES (1, 10, 2, 1, 1, 50.0, CURRENT_TIMESTAMP);
