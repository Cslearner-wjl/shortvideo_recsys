package com.shortvideo.recsys.backend.video;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shortvideo.recsys.backend.admin.AdminUserEntity;
import com.shortvideo.recsys.backend.admin.AdminUserMapper;
import com.shortvideo.recsys.backend.common.BizException;
import com.shortvideo.recsys.backend.common.ErrorCodes;
import com.shortvideo.recsys.backend.storage.MinioStorageService;
import com.shortvideo.recsys.backend.user.UserEntity;
import com.shortvideo.recsys.backend.user.UserMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("docker")
@EnabledIfEnvironmentVariable(named = "RUN_MINIO_IT", matches = "true")
class VideoMinioIntegrationTest {
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "Admin123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AdminUserMapper adminUserMapper;

    @Autowired
    private VideoMapper videoMapper;

    @Autowired
    private MinioStorageService minioStorageService;

    @BeforeEach
    void ensureAdminUser() {
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
    void upload_audit_feed_delete_shouldWork() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());
        String username = "uploader" + suffix;
        String email = "uploader" + suffix + "@example.com";
        long phoneTail = Math.abs(System.currentTimeMillis() % 100000000L);
        String phone = "136" + String.format("%08d", phoneTail);

        UserEntity uploader = new UserEntity();
        uploader.setUsername(username);
        uploader.setPhone(phone);
        uploader.setEmail(email);
        uploader.setPasswordHash(passwordEncoder.encode("Passw0rd!"));
        uploader.setStatus(1);
        userMapper.insert(uploader);

        String basicAdmin = basicAuth(ADMIN_USERNAME, ADMIN_PASSWORD);

        MockMultipartFile video = new MockMultipartFile(
                "video",
                "demo.mp4",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                "FAKE_MP4_BYTES".getBytes(StandardCharsets.UTF_8)
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/admin/videos")
                        .file(video)
                        .param("uploaderUserId", String.valueOf(uploader.getId()))
                        .param("title", "demo")
                        .header(HttpHeaders.AUTHORIZATION, basicAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andReturn();

        long videoId = readLong(uploadResult, "data", "id");
        VideoEntity entity = videoMapper.selectById(videoId);
        String objectKey = entity.getVideoUrl();
        minioStorageService.statObject(objectKey);

        mockMvc.perform(patch("/api/admin/videos/{id}/audit", videoId)
                        .header(HttpHeaders.AUTHORIZATION, basicAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)));

        mockMvc.perform(get("/api/videos/page")
                        .param("sort", "time")
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.total", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.items[0].id").value(videoId));

        mockMvc.perform(delete("/api/admin/videos/{id}", videoId)
                        .header(HttpHeaders.AUTHORIZATION, basicAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)));

        VideoEntity deleted = videoMapper.selectById(videoId);
        org.junit.jupiter.api.Assertions.assertNull(deleted);

        try {
            minioStorageService.statObject(objectKey);
            org.junit.jupiter.api.Assertions.fail("expected not found");
        } catch (BizException ex) {
            org.junit.jupiter.api.Assertions.assertEquals(ErrorCodes.RESOURCE_NOT_FOUND, ex.getCode());
        }
    }

    private long readLong(MvcResult result, String... path) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        for (String p : path) {
            node = node.get(p);
        }
        return node.asLong();
    }

    private String basicAuth(String username, String password) {
        String value = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
