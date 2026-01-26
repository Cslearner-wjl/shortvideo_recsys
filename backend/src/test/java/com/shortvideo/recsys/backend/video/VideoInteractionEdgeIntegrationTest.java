// 视频互动异常与幂等集成测试，覆盖重复操作与资源状态校验。
package com.shortvideo.recsys.backend.video;

import static org.hamcrest.Matchers.is;
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
import com.shortvideo.recsys.backend.common.ErrorCodes;
import com.shortvideo.recsys.backend.user.UserEntity;
import com.shortvideo.recsys.backend.user.UserMapper;
import com.shortvideo.recsys.backend.video.dto.CreateCommentRequest;
import java.time.LocalDateTime;
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
class VideoInteractionEdgeIntegrationTest {
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
    void duplicateLike_shouldBeIdempotent() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());
        String email = "edge-like" + suffix + "@example.com";
        String token = registerAndGetToken(email, "edge-like" + suffix, "139" + String.format("%08d", Math.abs(System.currentTimeMillis() % 100000000L)));

        UserEntity user = userMapper.selectOne(new QueryWrapper<UserEntity>().eq("email", email).last("limit 1"));
        Assertions.assertNotNull(user);

        VideoEntity video = createVideo(user.getId(), "edge-like" + suffix, "APPROVED");
        long videoId = video.getId();

        mockMvc.perform(post("/api/videos/{id}/like", videoId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)));

        mockMvc.perform(post("/api/videos/{id}/like", videoId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)));

        VideoStatsEntity stats = videoStatsMapper.selectById(videoId);
        Assertions.assertNotNull(stats);
        Assertions.assertEquals(1L, stats.getLikeCount());

        Long actionCount = userActionMapper.selectCount(new QueryWrapper<UserActionEntity>()
                .eq("user_id", user.getId())
                .eq("video_id", videoId)
                .eq("action_type", VideoInteractionService.ACTION_LIKE));
        Assertions.assertNotNull(actionCount);
        Assertions.assertEquals(1L, actionCount.longValue());
    }

    @Test
    void unapprovedVideo_like_shouldReturnNotFound() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());
        String email = "edge-pending" + suffix + "@example.com";
        String token = registerAndGetToken(email, "edge-p" + suffix, "138" + String.format("%08d", Math.abs(System.currentTimeMillis() % 100000000L)));

        UserEntity user = userMapper.selectOne(new QueryWrapper<UserEntity>().eq("email", email).last("limit 1"));
        Assertions.assertNotNull(user);

        VideoEntity video = createVideo(user.getId(), "edge-pending" + suffix, "PENDING");

        mockMvc.perform(post("/api/videos/{id}/like", video.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is(ErrorCodes.RESOURCE_NOT_FOUND)));
    }

    @Test
    void emptyComment_shouldReturnBadRequest() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());
        String email = "edge-comment" + suffix + "@example.com";
        String token = registerAndGetToken(email, "edge-c" + suffix, "137" + String.format("%08d", Math.abs(System.currentTimeMillis() % 100000000L)));

        UserEntity user = userMapper.selectOne(new QueryWrapper<UserEntity>().eq("email", email).last("limit 1"));
        Assertions.assertNotNull(user);

        VideoEntity video = createVideo(user.getId(), "edge-comment" + suffix, "APPROVED");

        mockMvc.perform(post("/api/videos/{id}/comments", video.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .content(objectMapper.writeValueAsString(new CreateCommentRequest("   "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is(ErrorCodes.BAD_REQUEST)))
                .andExpect(jsonPath("$.message", is("评论内容不能为空")));
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

    private VideoEntity createVideo(long uploaderUserId, String title, String auditStatus) {
        VideoEntity video = new VideoEntity();
        video.setUploaderUserId(uploaderUserId);
        video.setTitle(title);
        video.setDescription("demo");
        video.setVideoUrl("videos/" + title + ".mp4");
        video.setAuditStatus(auditStatus);
        video.setIsHot(0);
        video.setCreatedAt(LocalDateTime.now());
        videoMapper.insert(video);
        return video;
    }
}
