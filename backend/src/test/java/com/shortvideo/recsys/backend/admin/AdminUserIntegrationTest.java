// 管理端用户与管理员账号集成测试。
package com.shortvideo.recsys.backend.admin;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shortvideo.recsys.backend.admin.dto.AdminAccountRequest;
import com.shortvideo.recsys.backend.admin.dto.AdminUserStatusRequest;
import com.shortvideo.recsys.backend.user.UserEntity;
import com.shortvideo.recsys.backend.user.UserMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminUserIntegrationTest {
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "Admin123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AdminUserMapper adminUserMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setup() {
        AdminUserEntity admin = adminUserMapper.selectOne(new QueryWrapper<AdminUserEntity>()
                .eq("username", ADMIN_USERNAME)
                .last("limit 1"));
        if (admin == null) {
            AdminUserEntity entity = new AdminUserEntity();
            entity.setUsername(ADMIN_USERNAME);
            entity.setDisplayName("Admin");
            entity.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
            entity.setStatus(1);
            adminUserMapper.insert(entity);
            return;
        }

        admin.setDisplayName("Admin");
        admin.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
        admin.setStatus(1);
        adminUserMapper.updateById(admin);
    }

    @Test
    void listUsers_shouldSupportKeywordSearch() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());

        UserEntity u1 = new UserEntity();
        u1.setUsername("tom" + suffix);
        u1.setPhone("138" + String.format("%08d", Math.abs(System.currentTimeMillis() % 100000000L)));
        u1.setEmail("tom" + suffix + "@example.com");
        u1.setPasswordHash("hash");
        u1.setStatus(1);
        u1.setCreatedAt(LocalDateTime.now());
        userMapper.insert(u1);

        UserEntity u2 = new UserEntity();
        u2.setUsername("jerry" + suffix);
        u2.setPhone("139" + String.format("%08d", Math.abs((System.currentTimeMillis() + 1) % 100000000L)));
        u2.setEmail("jerry" + suffix + "@example.com");
        u2.setPasswordHash("hash");
        u2.setStatus(1);
        u2.setCreatedAt(LocalDateTime.now());
        userMapper.insert(u2);

        mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, basicAuth(ADMIN_USERNAME, ADMIN_PASSWORD))
                        .param("page", "1")
                        .param("size", "10")
                        .param("keyword", "tom" + suffix))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.total", is(1)))
                .andExpect(jsonPath("$.data.items[0].username", is("tom" + suffix)))
                .andExpect(jsonPath("$.data.items[0].email", is("tom" + suffix + "@example.com")))
                .andExpect(jsonPath("$.data.items[0].phone", is(u1.getPhone())));

        mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, basicAuth(ADMIN_USERNAME, ADMIN_PASSWORD))
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.total", not(0)));
    }

    @Test
    void updateStatus_shouldFreezeUser() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());
        UserEntity user = new UserEntity();
        user.setUsername("freeze" + suffix);
        user.setPhone("137" + String.format("%08d", Math.abs(System.currentTimeMillis() % 100000000L)));
        user.setEmail("freeze" + suffix + "@example.com");
        user.setPasswordHash("hash");
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        userMapper.insert(user);

        mockMvc.perform(patch("/api/admin/users/" + user.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, basicAuth(ADMIN_USERNAME, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminUserStatusRequest(0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)));

        UserEntity updated = userMapper.selectById(user.getId());
        Assertions.assertNotNull(updated);
        Assertions.assertEquals(0, updated.getStatus());
    }

    @Test
    void adminAccounts_shouldSupportCrud() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());
        AdminAccountRequest createRequest = new AdminAccountRequest("ops" + suffix, "Passw0rd!");

        String createBody = objectMapper.writeValueAsString(createRequest);
        String createResponse = mockMvc.perform(post("/api/admin/admins")
                        .header(HttpHeaders.AUTHORIZATION, basicAuth(ADMIN_USERNAME, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.username", is("ops" + suffix)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode createNode = objectMapper.readTree(createResponse);
        long adminId = createNode.path("data").path("id").asLong();
        Assertions.assertTrue(adminId > 0);

        AdminAccountRequest updateRequest = new AdminAccountRequest("ops" + suffix + "_new", "NewPass123");
        mockMvc.perform(put("/api/admin/admins/" + adminId)
                        .header(HttpHeaders.AUTHORIZATION, basicAuth(ADMIN_USERNAME, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.username", is("ops" + suffix + "_new")));

        mockMvc.perform(delete("/api/admin/admins/" + adminId)
                        .header(HttpHeaders.AUTHORIZATION, basicAuth(ADMIN_USERNAME, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)));
    }

    private String basicAuth(String username, String password) {
        String value = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
