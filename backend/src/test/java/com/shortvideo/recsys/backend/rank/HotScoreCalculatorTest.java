package com.shortvideo.recsys.backend.rank;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class HotScoreCalculatorTest {
    @Test
    void compute_shouldUseLog1pAndWeights() {
        HotScoreCalculator calc = new HotScoreCalculator();

        HotRankProperties.Weights w = new HotRankProperties.Weights();
        w.setPlay(1.0);
        w.setLike(3.0);
        w.setComment(5.0);
        w.setFavorite(4.0);

        double score = calc.compute(w, 99, 9, 1, 0);

        double expected = 1.0 * Math.log1p(99.0)
                + 3.0 * Math.log1p(9.0)
                + 5.0 * Math.log1p(1.0)
                + 4.0 * Math.log1p(0.0);

        Assertions.assertEquals(expected, score, 1e-9);
    }
}

