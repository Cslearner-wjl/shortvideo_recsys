// 用户资料更新接口集成测试。
package com.shortvideo.recsys.backend.user;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shortvideo.recsys.backend.auth.JwtService;
import com.shortvideo.recsys.backend.user.dto.UpdateProfileRequest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserProfileIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtService jwtService;

    @Test
    void updateProfile_shouldUpdateUserInfo() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());
        UserEntity user = new UserEntity();
        user.setUsername("profile" + suffix);
        user.setPhone("139" + String.format("%08d", Math.abs(System.currentTimeMillis() % 100000000L)));
        user.setEmail("profile" + suffix + "@example.com");
        user.setPasswordHash("hash");
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        userMapper.insert(user);

        String token = jwtService.generateToken(user.getId());
        UpdateProfileRequest request = new UpdateProfileRequest(
                "profile" + suffix + "_new",
                "138" + String.format("%08d", Math.abs((System.currentTimeMillis() + 1) % 100000000L)),
                "profile" + suffix + "_new@example.com",
                "https://example.com/avatar/" + suffix + ".png",
                "这是新的简介"
        );

        mockMvc.perform(put("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.user.username", is("profile" + suffix + "_new")))
                .andExpect(jsonPath("$.data.user.phone", is(request.phone())))
                .andExpect(jsonPath("$.data.user.email", is(request.email())))
                .andExpect(jsonPath("$.data.user.avatarUrl", is(request.avatarUrl())))
                .andExpect(jsonPath("$.data.user.bio", is(request.bio())));
    }
}
