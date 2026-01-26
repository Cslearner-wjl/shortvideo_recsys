// 评论服务，支持用户端与管理端评论分页与点赞。
package com.shortvideo.recsys.backend.video;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shortvideo.recsys.backend.common.BizException;
import com.shortvideo.recsys.backend.common.ErrorCodes;
import com.shortvideo.recsys.backend.user.UserEntity;
import com.shortvideo.recsys.backend.user.UserMapper;
import com.shortvideo.recsys.backend.video.dto.AdminCommentItemDto;
import com.shortvideo.recsys.backend.video.dto.AdminCommentUserDto;
import com.shortvideo.recsys.backend.video.dto.CommentItemDto;
import com.shortvideo.recsys.backend.video.dto.CommentUserDto;
import com.shortvideo.recsys.backend.video.dto.PageResponse;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile({"docker", "test"})
public class CommentService {
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CommentMapper commentMapper;
    private final CommentLikeMapper commentLikeMapper;
    private final UserMapper userMapper;
    private final VideoMapper videoMapper;

    public CommentService(
            CommentMapper commentMapper,
            CommentLikeMapper commentLikeMapper,
            UserMapper userMapper,
            VideoMapper videoMapper
    ) {
        this.commentMapper = commentMapper;
        this.commentLikeMapper = commentLikeMapper;
        this.userMapper = userMapper;
        this.videoMapper = videoMapper;
    }

    public PageResponse<CommentItemDto> pageForUser(long videoId, long page, long pageSize, String sort, Long viewerId) {
        requireApprovedVideo(videoId);
        return buildUserPage(videoId, page, pageSize, sort, viewerId);
    }

    public PageResponse<AdminCommentItemDto> pageForAdmin(long videoId, long page, long pageSize, String sort) {
        requireVideo(videoId);
        return buildAdminPage(videoId, page, pageSize, sort);
    }

    @Transactional
    public void like(long commentId, long userId) {
        CommentEntity comment = requireComment(commentId);
        requireApprovedVideo(comment.getVideoId());

        CommentLikeEntity entity = new CommentLikeEntity();
        entity.setCommentId(commentId);
        entity.setUserId(userId);
        try {
            if (commentLikeMapper.insert(entity) > 0) {
                commentMapper.incLike(commentId);
            }
        } catch (DuplicateKeyException ignored) {
        }
    }

    @Transactional
    public void unlike(long commentId, long userId) {
        CommentEntity comment = requireComment(commentId);
        requireApprovedVideo(comment.getVideoId());

        int deleted = commentLikeMapper.delete(new QueryWrapper<CommentLikeEntity>()
                .eq("comment_id", commentId)
                .eq("user_id", userId));
        if (deleted > 0) {
            commentMapper.decLike(commentId);
        }
    }

    private PageResponse<CommentItemDto> buildUserPage(long videoId, long page, long pageSize, String sort, Long viewerId) {
        Page<CommentEntity> pageResult = queryPage(videoId, page, pageSize, sort);
        List<CommentEntity> comments = pageResult.getRecords();
        Map<Long, UserEntity> users = loadUsers(comments);
        Set<Long> likedIds = loadLikedIds(viewerId, comments);

        List<CommentItemDto> items = comments.stream()
                .map(comment -> new CommentItemDto(
                        comment.getId(),
                        comment.getContent(),
                        comment.getCreatedAt() == null ? null : dtf.format(comment.getCreatedAt()),
                        comment.getLikeCount() == null ? 0L : comment.getLikeCount(),
                        likedIds.contains(comment.getId()),
                        toUserDto(users.get(comment.getUserId()))
                ))
                .toList();

        return new PageResponse<>(pageResult.getTotal(), pageResult.getCurrent(), pageResult.getSize(), items);
    }

    private PageResponse<AdminCommentItemDto> buildAdminPage(long videoId, long page, long pageSize, String sort) {
        Page<CommentEntity> pageResult = queryPage(videoId, page, pageSize, sort);
        List<CommentEntity> comments = pageResult.getRecords();
        Map<Long, UserEntity> users = loadUsers(comments);

        List<AdminCommentItemDto> items = comments.stream()
                .map(comment -> new AdminCommentItemDto(
                        comment.getId(),
                        comment.getContent(),
                        comment.getCreatedAt() == null ? null : dtf.format(comment.getCreatedAt()),
                        comment.getLikeCount() == null ? 0L : comment.getLikeCount(),
                        false,
                        toAdminUserDto(users.get(comment.getUserId()))
                ))
                .toList();

        return new PageResponse<>(pageResult.getTotal(), pageResult.getCurrent(), pageResult.getSize(), items);
    }

    private Page<CommentEntity> queryPage(long videoId, long page, long pageSize, String sort) {
        long safePage = page <= 0 ? 1 : page;
        long safeSize = pageSize <= 0 ? 20 : Math.min(pageSize, 50);

        QueryWrapper<CommentEntity> wrapper = new QueryWrapper<CommentEntity>()
                .eq("video_id", videoId);
        if ("time".equalsIgnoreCase(sort) || sort == null || sort.isBlank()) {
            wrapper.orderByDesc("created_at");
        } else {
            wrapper.orderByDesc("created_at");
        }
        return commentMapper.selectPage(new Page<>(safePage, safeSize), wrapper);
    }

    private Map<Long, UserEntity> loadUsers(List<CommentEntity> comments) {
        if (comments == null || comments.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> userIds = comments.stream()
                .map(CommentEntity::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));
    }

    private Set<Long> loadLikedIds(Long viewerId, List<CommentEntity> comments) {
        if (viewerId == null || viewerId <= 0 || comments == null || comments.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> commentIds = comments.stream()
                .map(CommentEntity::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (commentIds.isEmpty()) {
            return Collections.emptySet();
        }
        return commentLikeMapper.selectList(new QueryWrapper<CommentLikeEntity>()
                        .eq("user_id", viewerId)
                        .in("comment_id", commentIds))
                .stream()
                .map(CommentLikeEntity::getCommentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private CommentUserDto toUserDto(UserEntity user) {
        if (user == null) {
            return new CommentUserDto(0L, "Unknown", null);
        }
        return new CommentUserDto(user.getId(), user.getUsername(), user.getAvatarUrl());
    }

    private AdminCommentUserDto toAdminUserDto(UserEntity user) {
        if (user == null) {
            return new AdminCommentUserDto(0L, "Unknown", null, null, 0);
        }
        return new AdminCommentUserDto(
                user.getId(),
                user.getUsername(),
                user.getPhone(),
                user.getEmail(),
                user.getStatus() == null ? 0 : user.getStatus()
        );
    }

    private CommentEntity requireComment(long commentId) {
        CommentEntity comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "资源不存在");
        }
        return comment;
    }

    private void requireApprovedVideo(long videoId) {
        VideoEntity entity = videoMapper.selectOne(new QueryWrapper<VideoEntity>()
                .eq("id", videoId)
                .eq("audit_status", "APPROVED")
                .last("limit 1"));
        if (entity == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "资源不存在");
        }
    }

    private void requireVideo(long videoId) {
        VideoEntity entity = videoMapper.selectById(videoId);
        if (entity == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "资源不存在");
        }
    }
}
