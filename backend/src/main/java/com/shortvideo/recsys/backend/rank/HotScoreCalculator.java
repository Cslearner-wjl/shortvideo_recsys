package com.shortvideo.recsys.backend.rank;

import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * 热度计算器：对计数字段做 log1p 平滑，并按权重加权求和。
 */
@Component
public class HotScoreCalculator {
    public double compute(HotRankProperties.Weights weights, long play, long like, long comment, long favorite) {
        Objects.requireNonNull(weights, "weights");
        return weights.getPlay() * log1pNonNegative(play)
                + weights.getLike() * log1pNonNegative(like)
                + weights.getComment() * log1pNonNegative(comment)
                + weights.getFavorite() * log1pNonNegative(favorite);
    }

    private static double log1pNonNegative(long value) {
        long safe = Math.max(0L, value);
        return Math.log1p((double) safe);
    }
}

