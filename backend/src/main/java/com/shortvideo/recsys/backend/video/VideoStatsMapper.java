package com.shortvideo.recsys.backend.video;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface VideoStatsMapper extends BaseMapper<VideoStatsEntity> {
    @Update("UPDATE video_stats SET play_count = play_count + 1 WHERE video_id = #{videoId}")
    int incPlay(@Param("videoId") long videoId);

    @Update("UPDATE video_stats SET like_count = like_count + 1 WHERE video_id = #{videoId}")
    int incLike(@Param("videoId") long videoId);

    @Update("""
            UPDATE video_stats
            SET like_count = CASE WHEN like_count > 0 THEN like_count - 1 ELSE 0 END
            WHERE video_id = #{videoId}
            """)
    int decLike(@Param("videoId") long videoId);

    @Update("UPDATE video_stats SET favorite_count = favorite_count + 1 WHERE video_id = #{videoId}")
    int incFavorite(@Param("videoId") long videoId);

    @Update("""
            UPDATE video_stats
            SET favorite_count = CASE WHEN favorite_count > 0 THEN favorite_count - 1 ELSE 0 END
            WHERE video_id = #{videoId}
            """)
    int decFavorite(@Param("videoId") long videoId);

    @Update("UPDATE video_stats SET comment_count = comment_count + 1 WHERE video_id = #{videoId}")
    int incComment(@Param("videoId") long videoId);

    @Update("UPDATE video_stats SET hot_score = #{hotScore} WHERE video_id = #{videoId}")
    int updateHotScore(@Param("videoId") long videoId, @Param("hotScore") double hotScore);
}
