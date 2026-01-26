-- V6: 用户资料补充字段（头像/简介）

ALTER TABLE users
  ADD COLUMN avatar_url VARCHAR(512) NULL AFTER email,
  ADD COLUMN bio VARCHAR(255) NULL AFTER avatar_url;
