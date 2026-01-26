// 推荐接口边界集成测试，覆盖 pageSize 与 cursor 的异常输入。
package com.shortvideo.recsys.backend.recommendation;

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
class RecommendationBoundaryIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmailVerificationCodeMapper emailVerificationCodeMapper;

    @Test
    void pageSize_shouldClampToMax() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());
        String token = registerAndGetToken(
                "reco-boundary" + suffix + "@example.com",
                "reco-b" + suffix,
                "139" + String.format("%08d", Math.abs(System.currentTimeMillis() % 100000000L))
        );

        mockMvc.perform(get("/api/recommendations")
                        .param("page", "1")
                        .param("pageSize", "1000")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.pageSize", is(100)))
                .andExpect(jsonPath("$.data.page", is(1)));
    }

    @Test
    void cursor_invalid_shouldFallbackToFirstPage() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());
        String token = registerAndGetToken(
                "reco-cursor" + suffix + "@example.com",
                "reco-c" + suffix,
                "138" + String.format("%08d", Math.abs(System.currentTimeMillis() % 100000000L))
        );

        mockMvc.perform(get("/api/recommendations")
                        .param("page", "5")
                        .param("pageSize", "10")
                        .param("cursor", "abc")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.page", is(1)))
                .andExpect(jsonPath("$.data.pageSize", is(10)));
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
}
