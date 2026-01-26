// 管理端看板统计接口集成测试。
package com.shortvideo.recsys.backend.admin;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shortvideo.recsys.backend.user.UserEntity;
import com.shortvideo.recsys.backend.user.UserMapper;
import com.shortvideo.recsys.backend.video.UserActionEntity;
import com.shortvideo.recsys.backend.video.UserActionMapper;
import com.shortvideo.recsys.backend.video.VideoEntity;
import com.shortvideo.recsys.backend.video.VideoMapper;
import com.shortvideo.recsys.backend.video.VideoStatsEntity;
import com.shortvideo.recsys.backend.video.VideoStatsMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminAnalyticsIntegrationTest {
    private static final String ADMIN_USERNAME = "admin-analytics";
    private static final String ADMIN_PASSWORD = "AdminPass123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminUserMapper adminUserMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserActionMapper userActionMapper;

    @Autowired
    private VideoMapper videoMapper;

    @Autowired
    private VideoStatsMapper videoStatsMapper;

    @BeforeEach
    void setup() {
        AdminUserEntity admin = adminUserMapper.selectOne(new QueryWrapper<AdminUserEntity>()
                .eq("username", ADMIN_USERNAME)
                .last("limit 1"));
        if (admin == null) {
            AdminUserEntity entity = new AdminUserEntity();
            entity.setUsername(ADMIN_USERNAME);
            entity.setDisplayName("Admin");
            entity.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
            entity.setStatus(1);
            adminUserMapper.insert(entity);
        }
    }

    @Test
    void analyticsEndpoints_shouldReturnAggregatedData() throws Exception {
        LocalDate day1 = LocalDate.now().minusDays(2);
        LocalDate day2 = LocalDate.now().minusDays(1);

        UserEntity u1 = new UserEntity();
        u1.setUsername("a" + System.currentTimeMillis());
        u1.setPhone("136" + String.format("%08d", Math.abs(System.currentTimeMillis() % 100000000L)));
        u1.setEmail("a" + System.currentTimeMillis() + "@example.com");
        u1.setPasswordHash("hash");
        u1.setStatus(1);
        u1.setCreatedAt(day1.atTime(10, 0));
        userMapper.insert(u1);

        UserEntity u2 = new UserEntity();
        u2.setUsername("b" + System.currentTimeMillis());
        u2.setPhone("135" + String.format("%08d", Math.abs((System.currentTimeMillis() + 1) % 100000000L)));
        u2.setEmail("b" + System.currentTimeMillis() + "@example.com");
        u2.setPasswordHash("hash");
        u2.setStatus(1);
        u2.setCreatedAt(day2.atTime(11, 0));
        userMapper.insert(u2);

        long videoId = insertVideo(u1.getId(), day1.atTime(9, 0));
        insertStats(videoId);

        insertPlayAction(u1.getId(), videoId, day1.atTime(12, 0));
        insertPlayAction(u2.getId(), videoId, day2.atTime(12, 0));

        String from = day1.toString();
        String to = day2.toString();

        mockMvc.perform(get("/api/admin/analytics/daily-play")
                        .header(HttpHeaders.AUTHORIZATION, basicAuth(ADMIN_USERNAME, ADMIN_PASSWORD))
                        .param("from", from)
                        .param("to", to))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.length()", not(0)));

        mockMvc.perform(get("/api/admin/analytics/user-growth")
                        .header(HttpHeaders.AUTHORIZATION, basicAuth(ADMIN_USERNAME, ADMIN_PASSWORD))
                        .param("from", from)
                        .param("to", to))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.length()", not(0)));

        mockMvc.perform(get("/api/admin/analytics/active-users")
                        .header(HttpHeaders.AUTHORIZATION, basicAuth(ADMIN_USERNAME, ADMIN_PASSWORD))
                        .param("from", from)
                        .param("to", to))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.length()", not(0)));

        mockMvc.perform(get("/api/admin/analytics/video-publish")
                        .header(HttpHeaders.AUTHORIZATION, basicAuth(ADMIN_USERNAME, ADMIN_PASSWORD))
                        .param("from", from)
                        .param("to", to))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.length()", not(0)));

        mockMvc.perform(get("/api/admin/analytics/hot-topn")
                        .header(HttpHeaders.AUTHORIZATION, basicAuth(ADMIN_USERNAME, ADMIN_PASSWORD))
                        .param("n", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.length()", not(0)))
                .andExpect(jsonPath("$.data[0].videoId", is((int) videoId)));
    }

    private long insertVideo(long uploaderUserId, LocalDateTime createdAt) {
        VideoEntity video = new VideoEntity();
        video.setUploaderUserId(uploaderUserId);
        video.setTitle("t" + System.currentTimeMillis());
        video.setDescription("demo");
        video.setVideoUrl("videos/demo.mp4");
        video.setTags("[\"tag\"]");
        video.setAuditStatus("APPROVED");
        video.setIsHot(0);
        video.setCreatedAt(createdAt);
        videoMapper.insert(video);
        return video.getId();
    }

    private void insertStats(long videoId) {
        VideoStatsEntity stats = new VideoStatsEntity();
        stats.setVideoId(videoId);
        stats.setPlayCount(10L);
        stats.setLikeCount(2L);
        stats.setCommentCount(1L);
        stats.setFavoriteCount(1L);
        stats.setHotScore(50.0);
        videoStatsMapper.insert(stats);
    }

    private void insertPlayAction(long userId, long videoId, LocalDateTime time) {
        UserActionEntity action = new UserActionEntity();
        action.setUserId(userId);
        action.setVideoId(videoId);
        action.setActionType("PLAY");
        action.setActionTime(time);
        userActionMapper.insert(action);
    }

    private String basicAuth(String username, String password) {
        String value = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
