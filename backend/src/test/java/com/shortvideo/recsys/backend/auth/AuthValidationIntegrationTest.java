// 鉴权参数校验集成测试，覆盖邮箱、手机号与登录参数的非法输入。
package com.shortvideo.recsys.backend.auth;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shortvideo.recsys.backend.auth.dto.LoginRequest;
import com.shortvideo.recsys.backend.auth.dto.RegisterRequest;
import com.shortvideo.recsys.backend.auth.dto.SendEmailCodeRequest;
import com.shortvideo.recsys.backend.common.ErrorCodes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthValidationIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void sendEmailCode_invalidEmail_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/email-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendEmailCodeRequest("bad-email"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is(ErrorCodes.BAD_REQUEST)))
                .andExpect(jsonPath("$.message", is("邮箱格式不正确")));
    }

    @Test
    void register_invalidPhone_shouldReturnBadRequest() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "user",
                "123",
                "user@example.com",
                "Passw0rd!",
                "000000"
        );
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is(ErrorCodes.BAD_REQUEST)))
                .andExpect(jsonPath("$.message", is("手机号必须为11位数字")));
    }

    @Test
    void login_emptyAccount_shouldReturnBadRequest() throws Exception {
        LoginRequest request = new LoginRequest("", "");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is(ErrorCodes.BAD_REQUEST)))
                .andExpect(jsonPath("$.message", is("参数错误")));
    }
}
