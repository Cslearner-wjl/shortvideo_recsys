package com.shortvideo.recsys.backend.video;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shortvideo.recsys.backend.bigdata.BehaviorEventLogger;
import com.shortvideo.recsys.backend.common.BizException;
import com.shortvideo.recsys.backend.common.ErrorCodes;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Profile({"docker", "test"})
public class VideoInteractionService {
    public static final String ACTION_PLAY = "PLAY";
    public static final String ACTION_LIKE = "LIKE";
    public static final String ACTION_UNLIKE = "UNLIKE";
    public static final String ACTION_FAVORITE = "FAVORITE";
    public static final String ACTION_UNFAVORITE = "UNFAVORITE";
    public static final String ACTION_COMMENT = "COMMENT";

    private final VideoMapper videoMapper;
    private final VideoStatsMapper videoStatsMapper;
    private final VideoLikeMapper videoLikeMapper;
    private final VideoFavoriteMapper videoFavoriteMapper;
    private final CommentMapper commentMapper;
    private final UserActionMapper userActionMapper;
    private final BehaviorEventLogger behaviorEventLogger;
    private final Clock clock;
    private final TransactionTemplate txTemplate;

    private static final int DEFAULT_TX_RETRY_MAX_ATTEMPTS = 3;

    public VideoInteractionService(
            VideoMapper videoMapper,
            VideoStatsMapper videoStatsMapper,
            VideoLikeMapper videoLikeMapper,
            VideoFavoriteMapper videoFavoriteMapper,
            CommentMapper commentMapper,
            UserActionMapper userActionMapper,
            BehaviorEventLogger behaviorEventLogger,
            PlatformTransactionManager transactionManager
    ) {
        this.videoMapper = videoMapper;
        this.videoStatsMapper = videoStatsMapper;
        this.videoLikeMapper = videoLikeMapper;
        this.videoFavoriteMapper = videoFavoriteMapper;
        this.commentMapper = commentMapper;
        this.userActionMapper = userActionMapper;
        this.behaviorEventLogger = behaviorEventLogger;
        this.clock = Clock.system(ZoneId.of("Asia/Shanghai"));
        this.txTemplate = new TransactionTemplate(transactionManager);
        this.txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Transactional
    public void play(long videoId, long userId, Long durationMs, Boolean isCompleted) {
        requireApprovedVideo(videoId);
        ensureStatsRow(videoId);

        LocalDateTime now = LocalDateTime.now(clock);

        UserActionEntity action = new UserActionEntity();
        action.setUserId(userId);
        action.setVideoId(videoId);
        action.setActionType(ACTION_PLAY);
        action.setActionTime(now);
        action.setDurationMs(durationMs != null && durationMs > 0 ? durationMs : null);
        action.setIsCompleted(isCompleted == null ? null : (Boolean.TRUE.equals(isCompleted) ? 1 : 0));
        userActionMapper.insert(action);
        behaviorEventLogger.appendAction(
                ACTION_PLAY,
                userId,
                videoId,
                now,
                action.getDurationMs(),
                action.getIsCompleted() == null ? null : action.getIsCompleted() == 1
        );

        videoStatsMapper.incPlay(videoId);
    }

    @Transactional
    public void like(long videoId, long userId) {
        requireApprovedVideo(videoId);
        ensureStatsRow(videoId);

        VideoLikeEntity entity = new VideoLikeEntity();
        entity.setVideoId(videoId);
        entity.setUserId(userId);

        boolean changed = tryInsertLike(entity);
        if (!changed) {
            return;
        }

        videoStatsMapper.incLike(videoId);
        insertAction(userId, videoId, ACTION_LIKE);
    }

    @Transactional
    public void unlike(long videoId, long userId) {
        requireApprovedVideo(videoId);
        ensureStatsRow(videoId);

        int deleted = videoLikeMapper.delete(new QueryWrapper<VideoLikeEntity>()
                .eq("video_id", videoId)
                .eq("user_id", userId));
        if (deleted <= 0) {
            return;
        }

        videoStatsMapper.decLike(videoId);
        insertAction(userId, videoId, ACTION_UNLIKE);
    }

    @Transactional
    public void favorite(long videoId, long userId) {
        requireApprovedVideo(videoId);
        ensureStatsRow(videoId);

        VideoFavoriteEntity entity = new VideoFavoriteEntity();
        entity.setVideoId(videoId);
        entity.setUserId(userId);

        boolean changed = tryInsertFavorite(entity);
        if (!changed) {
            return;
        }

        videoStatsMapper.incFavorite(videoId);
        insertAction(userId, videoId, ACTION_FAVORITE);
    }

    @Transactional
    public void unfavorite(long videoId, long userId) {
        requireApprovedVideo(videoId);
        ensureStatsRow(videoId);

        int deleted = videoFavoriteMapper.delete(new QueryWrapper<VideoFavoriteEntity>()
                .eq("video_id", videoId)
                .eq("user_id", userId));
        if (deleted <= 0) {
            return;
        }

        videoStatsMapper.decFavorite(videoId);
        insertAction(userId, videoId, ACTION_UNFAVORITE);
    }

    public void comment(long videoId, long userId, String content) {
        String safeContent = content == null ? null : content.trim();
        if (safeContent == null || safeContent.isEmpty()) {
            throw new BizException(ErrorCodes.BAD_REQUEST, "评论内容不能为空");
        }
        if (safeContent.length() > 1000) {
            throw new BizException(ErrorCodes.BAD_REQUEST, "评论内容过长");
        }

        // 高并发下 InnoDB 可能出现死锁/锁等待失败；对整个事务做有限次重试以降低 500。
        runWithTxRetry(() -> {
            requireApprovedVideo(videoId);
            ensureStatsRow(videoId);

            CommentEntity comment = new CommentEntity();
            comment.setVideoId(videoId);
            comment.setUserId(userId);
            comment.setContent(safeContent);
            commentMapper.insert(comment);

            videoStatsMapper.incComment(videoId);
            insertAction(userId, videoId, ACTION_COMMENT);
        });
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

    private void ensureStatsRow(long videoId) {
        // 大多数情况下 stats 行已存在：先读后插，避免“重复插入 + 更新同一行”触发 S->X 锁升级死锁。
        if (videoStatsMapper.selectById(videoId) != null) {
            return;
        }
        try {
            VideoStatsEntity stats = new VideoStatsEntity();
            stats.setVideoId(videoId);
            stats.setPlayCount(0L);
            stats.setLikeCount(0L);
            stats.setCommentCount(0L);
            stats.setFavoriteCount(0L);
            stats.setHotScore(0.0);
            videoStatsMapper.insert(stats);
        } catch (DuplicateKeyException ignored) {
        }
    }

    private boolean tryInsertLike(VideoLikeEntity entity) {
        try {
            return videoLikeMapper.insert(entity) > 0;
        } catch (DuplicateKeyException ignored) {
            return false;
        }
    }

    private boolean tryInsertFavorite(VideoFavoriteEntity entity) {
        try {
            return videoFavoriteMapper.insert(entity) > 0;
        } catch (DuplicateKeyException ignored) {
            return false;
        }
    }

    private void insertAction(long userId, long videoId, String type) {
        Objects.requireNonNull(type, "type");
        UserActionEntity action = new UserActionEntity();
        action.setUserId(userId);
        action.setVideoId(videoId);
        action.setActionType(type);
        LocalDateTime now = LocalDateTime.now(clock);
        action.setActionTime(now);
        userActionMapper.insert(action);
        behaviorEventLogger.appendAction(type, userId, videoId, now, null, null);
    }

    /**
     * 事务级重试包装器：仅对明确的并发锁冲突类异常执行有限次重试。
     * 每次 attempt 都使用 REQUIRES_NEW 开启独立事务，确保异常时完整回滚后再重试。
     */
    protected void runWithTxRetry(Runnable task) {
        int attempt = 0;
        while (true) {
            attempt++;
            try {
                txTemplate.execute(status -> {
                    task.run();
                    return null;
                });
                return;
            } catch (DeadlockLoserDataAccessException | CannotAcquireLockException ex) {
                if (attempt >= DEFAULT_TX_RETRY_MAX_ATTEMPTS) {
                    throw ex;
                }
                backoff(attempt);
            }
        }
    }

    private static void backoff(int attempt) {
        // 指数退避 + 抖动，避免并发重试风暴。范围控制在 10~200ms。
        long base = (long) Math.min(200, 10 * Math.pow(2, attempt - 1));
        long sleepMs = ThreadLocalRandom.current().nextLong(base / 2 + 1, base + 1);
        try {
            Thread.sleep(sleepMs);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
