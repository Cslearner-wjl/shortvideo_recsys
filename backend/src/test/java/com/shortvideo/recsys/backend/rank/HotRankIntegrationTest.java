package com.shortvideo.recsys.backend.rank;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shortvideo.recsys.backend.user.UserEntity;
import com.shortvideo.recsys.backend.user.UserMapper;
import com.shortvideo.recsys.backend.video.VideoEntity;
import com.shortvideo.recsys.backend.video.VideoMapper;
import com.shortvideo.recsys.backend.video.VideoStatsEntity;
import com.shortvideo.recsys.backend.video.VideoStatsMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HotRankIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HotRankService hotRankService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private VideoMapper videoMapper;

    @Autowired
    private VideoStatsMapper videoStatsMapper;

    @Test
    void refresh_thenRankHot_shouldReturnByHotScoreDesc() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());

        UserEntity uploader = new UserEntity();
        uploader.setUsername("rank" + suffix);
        uploader.setPhone("138" + String.format("%08d", Math.abs(System.currentTimeMillis() % 100000000L)));
        uploader.setEmail("rank" + suffix + "@example.com");
        uploader.setPasswordHash(passwordEncoder.encode("Passw0rd!"));
        uploader.setStatus(1);
        userMapper.insert(uploader);

        long uid = uploader.getId();

        long v1 = insertApprovedVideo(uid, "v1");
        long v2 = insertApprovedVideo(uid, "v2");

        upsertStats(v1, 100, 1, 0, 0);
        upsertStats(v2, 5, 10, 2, 1);

        hotRankService.refresh();

        MvcResult result = mockMvc.perform(get("/api/rank/hot")
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.total", greaterThanOrEqualTo(2)))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode items = root.get("data").get("items");
        double s0 = items.get(0).get("hotScore").asDouble();
        double s1 = items.get(1).get("hotScore").asDouble();
        org.junit.jupiter.api.Assertions.assertTrue(s0 >= s1);
    }

    private long insertApprovedVideo(long uploaderUserId, String title) {
        VideoEntity video = new VideoEntity();
        video.setUploaderUserId(uploaderUserId);
        video.setTitle(title);
        video.setDescription("demo");
        video.setVideoUrl("videos/" + title + ".mp4");
        video.setAuditStatus("APPROVED");
        video.setIsHot(0);
        video.setCreatedAt(LocalDateTime.now());
        videoMapper.insert(video);
        return video.getId();
    }

    private void upsertStats(long videoId, long play, long like, long comment, long favorite) {
        VideoStatsEntity stats = videoStatsMapper.selectById(videoId);
        if (stats == null) {
            stats = new VideoStatsEntity();
            stats.setVideoId(videoId);
            stats.setPlayCount(play);
            stats.setLikeCount(like);
            stats.setCommentCount(comment);
            stats.setFavoriteCount(favorite);
            stats.setHotScore(0.0);
            videoStatsMapper.insert(stats);
            return;
        }
        stats.setPlayCount(play);
        stats.setLikeCount(like);
        stats.setCommentCount(comment);
        stats.setFavoriteCount(favorite);
        videoStatsMapper.updateById(stats);
    }
}
