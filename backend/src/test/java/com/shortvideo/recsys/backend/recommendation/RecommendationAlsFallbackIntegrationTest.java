package com.shortvideo.recsys.backend.recommendation;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb_als_fallback;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
        "app.recommendations.als.enabled=true",
        "app.recommendations.als.redis-prefix=rec:user:"
})
class RecommendationAlsFallbackIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmailVerificationCodeMapper emailVerificationCodeMapper;

    @Autowired
    private VideoMapper videoMapper;

    @Autowired
    private VideoStatsMapper videoStatsMapper;

    @Test
    void alsEnabled_butRedisMissing_shouldFallbackToRuleRecommendation() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());
        String token = registerAndGetToken("als" + suffix + "@example.com", "als" + suffix, "137" + String.format("%08d", Math.abs(System.currentTimeMillis() % 100000000L)));

        long v1 = insertApprovedVideo("alsVideo1" + suffix, "[\"a\"]");
        upsertStats(v1, 10, 1, 0, 0);

        mockMvc.perform(get("/api/recommendations")
                        .param("page", "1")
                        .param("pageSize", "10")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.items.length()", greaterThanOrEqualTo(1)));
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
        Assertions.assertNotNull(codeEntity);

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

    private long insertApprovedVideo(String title, String tags) {
        VideoEntity video = new VideoEntity();
        video.setUploaderUserId(1L);
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
}
