package com.shortvideo.recsys.backend.auth;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shortvideo.recsys.backend.auth.dto.LoginRequest;
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
class AuthFlowIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmailVerificationCodeMapper emailVerificationCodeMapper;

    @Test
    void register_login_me_shouldWork() throws Exception {
        String email = "u1@example.com";
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
                "u1",
                "13800138000",
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
                .andExpect(jsonPath("$.data.user.username", is("u1")))
                .andReturn();

        String token = readToken(regResult);

        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.user.username", is("u1")));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "Passw0rd!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andReturn();

        String token2 = readToken(loginResult);
        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.user.email", is(email)));
    }

    @Test
    void login_wrongPassword_shouldReturnGenericError() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("not-exists", "bad"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is(40101)))
                .andExpect(jsonPath("$.message", is("账号或密码错误")));
    }

    private String readToken(MvcResult result) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("data").get("token").asText();
    }
}

