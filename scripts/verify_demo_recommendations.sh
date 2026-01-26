#!/bin/bash
# 用途: 验证演示推荐结果
# 用法: bash scripts/verify_demo_recommendations.sh

set -e

# Configuration
API_BASE="http://localhost:18080"
MYSQL_HOST="127.0.0.1"
MYSQL_PORT="3307"
MYSQL_USER="app"
MYSQL_PASS="apppass"
DB_NAME="shortvideo_recsys"

# 1. Register/Login User
echo "=== 1. Preparing User ==="
USERNAME="demoUser_$(date +%s)"
PASSWORD="DemoPass123!"
PHONE="139$(date +%s | tail -c 8)"
EMAIL="${USERNAME}@example.com"

# Send pseudo email code
curl -s -X POST "$API_BASE/api/auth/email-code" \
  -H "Content-Type: application/json" \
  -d "{\"email\": \"$EMAIL\"}" > /dev/null

# Get code from DB
EMAIL_CODE=$(docker exec shortvideo-recsys-mysql mysql -u"$MYSQL_USER" -p"$MYSQL_PASS" "$DB_NAME" -N -e "SELECT code FROM email_verification_codes WHERE email='$EMAIL' ORDER BY created_at DESC LIMIT 1")

# Register
echo "Registering $USERNAME..."
REGISTER_RES=$(curl -s -X POST "$API_BASE/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"username\": \"$USERNAME\", \"password\": \"$PASSWORD\", \"phone\": \"$PHONE\", \"email\": \"$EMAIL\", \"emailCode\": \"$EMAIL_CODE\"}")
echo "Response: $REGISTER_RES"

TOKEN=$(echo $REGISTER_RES | python3 -c "import sys, json; print(json.load(sys.stdin)['data']['token'])")
USER_ID=$(echo $REGISTER_RES | python3 -c "import sys, json; print(json.load(sys.stdin)['data']['user']['id'])")

echo "User created. ID: $USER_ID"

# 2. Insert Demo Data
echo "=== 2. Inserting Demo Data ==="
SUFFIX=$(date +%s)
cat <<EOF > /tmp/demo_seed.sql
SET @userId = $USER_ID;
INSERT INTO videos(uploader_user_id, title, description, video_url, tags, audit_status, is_hot) VALUES
(@userId, 'sports_seen_$SUFFIX', 'demo', 'videos/sports_seen_$SUFFIX.mp4', '["sports"]', 'APPROVED', 0),
(@userId, 'sports_cand_$SUFFIX', 'demo', 'videos/sports_cand_$SUFFIX.mp4', '["sports"]', 'APPROVED', 0),
(@userId, 'music_seen_$SUFFIX', 'demo', 'videos/music_seen_$SUFFIX.mp4', '["music"]', 'APPROVED', 0),
(@userId, 'music_cand_$SUFFIX', 'demo', 'videos/music_cand_$SUFFIX.mp4', '["music"]', 'APPROVED', 0);

SET @v1 = LAST_INSERT_ID();
SET @v2 = @v1 + 1;
SET @v3 = @v1 + 2;
SET @v4 = @v1 + 3;

INSERT INTO video_stats(video_id, play_count, like_count, comment_count, favorite_count, hot_score) VALUES
(@v1, 10, 1, 0, 0, 0),
(@v2, 2, 0, 0, 0, 0),
(@v3, 100, 10, 5, 1, 0),
(@v4, 1, 0, 0, 0, 0);

-- User likes one sports video
INSERT INTO user_actions(user_id, video_id, action_type, action_time) VALUES
(@userId, @v1, 'FAVORITE', NOW(3));
EOF

docker exec -i shortvideo-recsys-mysql mysql -u"$MYSQL_USER" -p"$MYSQL_PASS" "$DB_NAME" < /tmp/demo_seed.sql

# 3. Refresh Hot Rank (Admin)
echo "=== 3. Refreshing Hot Rank ==="
# Admin credentials: default is admin/AdminPass123
curl -s -X POST "$API_BASE/api/admin/rank/hot/refresh" \
  -u "admin:AdminPass123"

# 4. Get Recommendations
echo -e "\n=== 4. Getting Recommendations ==="
curl -s "$API_BASE/api/recommendations?page=1&pageSize=5" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool

echo -e "\n\nDone! If 'items' is not empty, step 4.1 is verified."
