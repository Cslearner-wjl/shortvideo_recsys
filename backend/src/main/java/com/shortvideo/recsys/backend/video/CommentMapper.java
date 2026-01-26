package com.shortvideo.recsys.backend.video;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CommentMapper extends BaseMapper<CommentEntity> {
    @Update("UPDATE comments SET like_count = like_count + 1 WHERE id = #{commentId}")
    int incLike(long commentId);

    @Update("UPDATE comments SET like_count = CASE WHEN like_count > 0 THEN like_count - 1 ELSE 0 END WHERE id = #{commentId}")
    int decLike(long commentId);
}
