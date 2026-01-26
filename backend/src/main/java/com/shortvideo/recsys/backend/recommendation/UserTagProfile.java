package com.shortvideo.recsys.backend.recommendation;

import java.util.List;
import java.util.Map;

public record UserTagProfile(Map<String, Integer> tagScores, List<String> topTags) {
    public boolean isEmpty() {
        return topTags == null || topTags.isEmpty();
    }
}

