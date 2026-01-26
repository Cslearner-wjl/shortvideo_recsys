// 热门榜单分页边界集成测试，覆盖 page/pageSize 的非法输入。
package com.shortvideo.recsys.backend.rank;

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
class HotRankBoundaryIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void pageSize_shouldClampToMax() throws Exception {
        mockMvc.perform(get("/api/rank/hot")
                        .param("page", "1")
                        .param("pageSize", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.pageSize", is(100)));
    }

    @Test
    void page_shouldFallbackToOne_whenNonPositive() throws Exception {
        mockMvc.perform(get("/api/rank/hot")
                        .param("page", "0")
                        .param("pageSize", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.page", is(1)))
                .andExpect(jsonPath("$.data.pageSize", is(20)));
    }
}
