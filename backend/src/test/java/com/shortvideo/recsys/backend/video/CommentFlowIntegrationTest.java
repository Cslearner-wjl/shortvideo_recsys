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
import java.util.Map;
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
class CommentFlowIntegrationTest {
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

    @Test
    void comment_like_shouldUpdateList() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());
        String email = "comment" + suffix + "@example.com";
        String username = "comment" + suffix;
        String phone = "138" + String.format("%08d", Math.abs(System.currentTimeMillis() % 100000000L));

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

        mockMvc.perform(post("/api/videos/{id}/comments", videoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .content(objectMapper.writeValueAsString(Map.of("content", "hello"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)));

        MvcResult listResult = mockMvc.perform(get("/api/videos/{id}/comments", videoId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.items[0].content", is("hello")))
                .andExpect(jsonPath("$.data.items[0].likeCount", is(0)))
                .andExpect(jsonPath("$.data.items[0].liked", is(false)))
                .andReturn();

        long commentId = readFirstCommentId(listResult);

        mockMvc.perform(post("/api/comments/{id}/likes", commentId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)));

        mockMvc.perform(get("/api/videos/{id}/comments", videoId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.items[0].likeCount", is(1)))
                .andExpect(jsonPath("$.data.items[0].liked", is(true)));
    }

    private String readToken(MvcResult result) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("data").get("token").asText();
    }

    private long readFirstCommentId(MvcResult result) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("data").get("items").get(0).get("id").asLong();
    }
}
