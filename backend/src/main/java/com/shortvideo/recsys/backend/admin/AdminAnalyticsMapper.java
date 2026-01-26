// 管理端看板聚合查询 Mapper，封装统计 SQL。
package com.shortvideo.recsys.backend.admin;

import com.shortvideo.recsys.backend.admin.dto.ActiveUserDto;
import com.shortvideo.recsys.backend.admin.dto.DailyPlayDto;
import com.shortvideo.recsys.backend.admin.dto.HotTopnDto;
import com.shortvideo.recsys.backend.admin.dto.UserGrowthDto;
import com.shortvideo.recsys.backend.admin.dto.VideoPublishDto;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AdminAnalyticsMapper {
    @Results({
            @Result(column = "day_value", property = "day")
    })
    @Select("""
            SELECT CAST(action_time AS DATE) AS day_value, COUNT(*) AS playCount
            FROM user_actions
            WHERE action_type = 'PLAY'
              AND action_time >= #{fromStart}
              AND action_time < #{toEnd}
            GROUP BY CAST(action_time AS DATE)
            ORDER BY day_value
            """)
    List<DailyPlayDto> selectDailyPlay(@Param("fromStart") LocalDateTime fromStart, @Param("toEnd") LocalDateTime toEnd);

    @Results({
            @Result(column = "day_value", property = "day")
    })
    @Select("""
            SELECT CAST(created_at AS DATE) AS day_value, COUNT(*) AS newUserCount
            FROM users
            WHERE created_at >= #{fromStart}
              AND created_at < #{toEnd}
            GROUP BY CAST(created_at AS DATE)
            ORDER BY day_value
            """)
    List<UserGrowthDto> selectUserGrowth(@Param("fromStart") LocalDateTime fromStart, @Param("toEnd") LocalDateTime toEnd);

    @Results({
            @Result(column = "day_value", property = "day")
    })
    @Select("""
            SELECT CAST(action_time AS DATE) AS day_value, COUNT(DISTINCT user_id) AS activeUserCount
            FROM user_actions
            WHERE action_time >= #{fromStart}
              AND action_time < #{toEnd}
            GROUP BY CAST(action_time AS DATE)
            ORDER BY day_value
            """)
    List<ActiveUserDto> selectActiveUsers(@Param("fromStart") LocalDateTime fromStart, @Param("toEnd") LocalDateTime toEnd);

    @Select("""
            SELECT v.id AS videoId,
                   v.title AS title,
                   vs.play_count AS playCount,
                   vs.like_count AS likeCount,
                   vs.comment_count AS commentCount,
                   vs.favorite_count AS favoriteCount,
                   vs.hot_score AS hotScore
            FROM video_stats vs
            JOIN videos v ON v.id = vs.video_id
            ORDER BY vs.hot_score DESC, vs.play_count DESC
            LIMIT #{limit}
            """)
    List<HotTopnDto> selectHotTopn(@Param("limit") int limit);

    @Results({
            @Result(column = "day_value", property = "day")
    })
    @Select("""
            SELECT CAST(created_at AS DATE) AS day_value, COUNT(*) AS publishCount
            FROM videos
            WHERE created_at >= #{fromStart}
              AND created_at < #{toEnd}
            GROUP BY CAST(created_at AS DATE)
            ORDER BY day_value
            """)
    List<VideoPublishDto> selectVideoPublish(@Param("fromStart") LocalDateTime fromStart, @Param("toEnd") LocalDateTime toEnd);
}
