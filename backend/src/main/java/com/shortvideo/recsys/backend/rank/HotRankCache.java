package com.shortvideo.recsys.backend.rank;

import java.util.List;

public interface HotRankCache {
    void replaceTop(List<HotRankEntry> entries);

    List<Long> getIds(long startInclusive, long endInclusive);

    long size();
}

