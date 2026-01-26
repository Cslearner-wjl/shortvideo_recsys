package com.shortvideo.recsys.backend.video;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
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
import com.shortvideo.recsys.backend.user.UserEntity;
import com.shortvideo.recsys.backend.user.UserMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VideoLikeFlowIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmailVerificationCodeMapper emailVerificationCodeMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private VideoMapper videoMapper;

    @Autowired
    private VideoStatsMapper videoStatsMapper;

    @Autowired
    private UserActionMapper userActionMapper;

    @Test
    void login_like_shouldUpdateCount() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());
        String email = "like" + suffix + "@example.com";
        String username = "like" + suffix;
        String phone = "139" + String.format("%08d", Math.abs(System.currentTimeMillis() % 100000000L));

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
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andReturn();

        String token = readToken(regResult);

        UserEntity user = userMapper.selectOne(new QueryWrapper<UserEntity>().eq("email", email).last("limit 1"));
        Assertions.assertNotNull(user);
        Assertions.assertNotNull(user.getId());

        VideoEntity video = new VideoEntity();
        video.setUploaderUserId(user.getId());
        video.setTitle("demo");
        video.setDescription("demo");
        video.setVideoUrl("videos/demo.mp4");
        video.setAuditStatus("APPROVED");
        video.setIsHot(0);
        videoMapper.insert(video);

        long videoId = video.getId();

        mockMvc.perform(post("/api/videos/{id}/like", videoId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)));

        VideoStatsEntity stats = videoStatsMapper.selectById(videoId);
        Assertions.assertNotNull(stats);
        Assertions.assertEquals(1L, stats.getLikeCount());

        mockMvc.perform(get("/api/videos/{id}", videoId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.liked", is(true)))
                .andExpect(jsonPath("$.data.favorited", is(false)));

        Long actionCount = userActionMapper.selectCount(new QueryWrapper<UserActionEntity>()
                .eq("user_id", user.getId())
                .eq("video_id", videoId)
                .eq("action_type", VideoInteractionService.ACTION_LIKE));
        Assertions.assertNotNull(actionCount);
        Assertions.assertEquals(1L, actionCount.longValue());
    }

    private String readToken(MvcResult result) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("data").get("token").asText();
    }
}
