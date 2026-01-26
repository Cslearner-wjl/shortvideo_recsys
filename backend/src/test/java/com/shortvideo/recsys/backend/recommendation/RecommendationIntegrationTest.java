package com.shortvideo.recsys.backend.recommendation;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shortvideo.recsys.backend.auth.EmailVerificationCodeEntity;
import com.shortvideo.recsys.backend.auth.EmailVerificationCodeMapper;
import com.shortvideo.recsys.backend.auth.dto.RegisterRequest;
import com.shortvideo.recsys.backend.auth.dto.SendEmailCodeRequest;
import com.shortvideo.recsys.backend.rank.HotRankService;
import com.shortvideo.recsys.backend.user.UserEntity;
import com.shortvideo.recsys.backend.user.UserMapper;
import com.shortvideo.recsys.backend.video.UserActionEntity;
import com.shortvideo.recsys.backend.video.UserActionMapper;
import com.shortvideo.recsys.backend.video.VideoEntity;
import com.shortvideo.recsys.backend.video.VideoMapper;
import com.shortvideo.recsys.backend.video.VideoStatsEntity;
import com.shortvideo.recsys.backend.video.VideoStatsMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecommendationIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmailVerificationCodeMapper emailVerificationCodeMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private VideoMapper videoMapper;

    @Autowired
    private VideoStatsMapper videoStatsMapper;

    @Autowired
    private UserActionMapper userActionMapper;

    @Autowired
    private HotRankService hotRankService;

    @Test
    void existingUser_shouldPreferTags_andDedupSeenVideos() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());

        String token = registerAndGetToken("reco" + suffix + "@example.com", "reco" + suffix, "139" + String.format("%08d", Math.abs(System.currentTimeMillis() % 100000000L)));
        UserEntity user = userMapper.selectOne(new QueryWrapper<UserEntity>().eq("email", "reco" + suffix + "@example.com").last("limit 1"));
        Assertions.assertNotNull(user);

        long sportsSeen = insertApprovedVideo(user.getId(), "sportsSeen" + suffix, "[\"sports\"]");
        long sportsCandidate = insertApprovedVideo(user.getId(), "sportsCandidate" + suffix, "[\"sports\"]");
        long musicCandidate = insertApprovedVideo(user.getId(), "musicCandidate" + suffix, "[\"music\"]");

        upsertStats(sportsSeen, 10, 1, 0, 0);
        upsertStats(sportsCandidate, 1, 0, 0, 0);
        upsertStats(musicCandidate, 100, 10, 5, 1);

        hotRankService.refresh();

        insertAction(user.getId(), sportsSeen, "FAVORITE");

        MvcResult result = mockMvc.perform(get("/api/recommendations")
                        .param("page", "1")
                        .param("pageSize", "20")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.items[0].liked", is(false)))
                .andExpect(jsonPath("$.data.items[0].favorited", is(false)))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode items = root.get("data").get("items");
        Assertions.assertTrue(items.size() > 0);

        long firstId = items.get(0).get("id").asLong();
        JsonNode firstTags = items.get(0).get("tags");
        Assertions.assertTrue(firstTags.isArray());
        Assertions.assertTrue(firstTags.toString().contains("sports"));
        Assertions.assertNotEquals(sportsSeen, firstId);

        for (JsonNode it : items) {
            Assertions.assertNotEquals(sportsSeen, it.get("id").asLong());
        }
    }

    @Test
    void newUser_shouldReturnSomeItems() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());

        String token = registerAndGetToken("new" + suffix + "@example.com", "new" + suffix, "138" + String.format("%08d", Math.abs(System.currentTimeMillis() % 100000000L)));
        UserEntity user = userMapper.selectOne(new QueryWrapper<UserEntity>().eq("email", "new" + suffix + "@example.com").last("limit 1"));
        Assertions.assertNotNull(user);

        long v1 = insertApprovedVideo(user.getId(), "h1" + suffix, "[\"a\"]");
        long v2 = insertApprovedVideo(user.getId(), "h2" + suffix, "[\"b\"]");
        upsertStats(v1, 100, 10, 1, 1);
        upsertStats(v2, 10, 1, 0, 0);
        hotRankService.refresh();

        mockMvc.perform(get("/api/recommendations")
                        .param("page", "1")
                        .param("pageSize", "10")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.items[0].liked", is(false)))
                .andExpect(jsonPath("$.data.items[0].favorited", is(false)))
                .andExpect(jsonPath("$.data.items.length()", greaterThanOrEqualTo(2)));
    }

    private String registerAndGetToken(String email, String username, String phone) throws Exception {
        mockMvc.perform(post("/api/auth/email-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendEmailCodeRequest(email))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)));

        EmailVerificationCodeEntity codeEntity = emailVerificationCodeMapper.selectOne(
                new QueryWrapper<EmailVerificationCodeEntity>()
                        .eq("email", email)
                        .eq("purpose", "REGISTER")
                        .orderByDesc("created_at")
                        .last("limit 1")
        );

        RegisterRequest registerRequest = new RegisterRequest(
                username,
                phone,
                email,
                "Passw0rd!",
                codeEntity.getCode()
        );

        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andReturn();

        JsonNode node = objectMapper.readTree(regResult.getResponse().getContentAsString());
        return node.get("data").get("token").asText();
    }

    private long insertApprovedVideo(long uploaderUserId, String title, String tags) {
        VideoEntity video = new VideoEntity();
        video.setUploaderUserId(uploaderUserId);
        video.setTitle(title);
        video.setDescription("demo");
        video.setVideoUrl("videos/" + title + ".mp4");
        video.setTags(tags);
        video.setAuditStatus("APPROVED");
        video.setIsHot(0);
        video.setCreatedAt(LocalDateTime.now());
        videoMapper.insert(video);

        VideoStatsEntity stats = new VideoStatsEntity();
        stats.setVideoId(video.getId());
        stats.setPlayCount(0L);
        stats.setLikeCount(0L);
        stats.setCommentCount(0L);
        stats.setFavoriteCount(0L);
        stats.setHotScore(0.0);
        videoStatsMapper.insert(stats);

        return video.getId();
    }

    private void upsertStats(long videoId, long play, long like, long comment, long favorite) {
        VideoStatsEntity stats = videoStatsMapper.selectById(videoId);
        Assertions.assertNotNull(stats);
        stats.setPlayCount(play);
        stats.setLikeCount(like);
        stats.setCommentCount(comment);
        stats.setFavoriteCount(favorite);
        videoStatsMapper.updateById(stats);
    }

    private void insertAction(long userId, long videoId, String type) {
        UserActionEntity action = new UserActionEntity();
        action.setUserId(userId);
        action.setVideoId(videoId);
        action.setActionType(type);
        action.setActionTime(LocalDateTime.now());
        userActionMapper.insert(action);
    }
}
