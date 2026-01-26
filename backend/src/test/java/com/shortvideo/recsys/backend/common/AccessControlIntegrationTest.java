// 权限访问控制集成测试，验证未登录访问受限接口的返回。
package com.shortvideo.recsys.backend.common;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccessControlIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void unauthenticated_userEndpoints_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is(ErrorCodes.UNAUTHORIZED)))
                .andExpect(jsonPath("$.message", is("未登录或Token无效")));

        mockMvc.perform(get("/api/recommendations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is(ErrorCodes.UNAUTHORIZED)))
                .andExpect(jsonPath("$.message", is("未登录或Token无效")));
    }

    @Test
    void adminEndpoint_withoutBasic_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }
}
