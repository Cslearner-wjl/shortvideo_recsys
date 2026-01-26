package com.shortvideo.recsys.backend.recommendation;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.recommendations")
public class RecommendationProperties {
    private boolean enabled = false;
    private int candidatePoolSize = 2000;
    private int recentActionsLimit = 200;
    private int topTags = 5;
    private double tagRatio = 0.7;
    private double hotRatio = 0.2;
    private double randomRatio = 0.1;
    private long randomSeed = 2026L;
    private final ActionWeights actionWeights = new ActionWeights();
    private final Als als = new Als();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getCandidatePoolSize() {
        return candidatePoolSize;
    }

    public void setCandidatePoolSize(int candidatePoolSize) {
        this.candidatePoolSize = candidatePoolSize;
    }

    public int getRecentActionsLimit() {
        return recentActionsLimit;
    }

    public void setRecentActionsLimit(int recentActionsLimit) {
        this.recentActionsLimit = recentActionsLimit;
    }

    public int getTopTags() {
        return topTags;
    }

    public void setTopTags(int topTags) {
        this.topTags = topTags;
    }

    public double getTagRatio() {
        return tagRatio;
    }

    public void setTagRatio(double tagRatio) {
        this.tagRatio = tagRatio;
    }

    public double getHotRatio() {
        return hotRatio;
    }

    public void setHotRatio(double hotRatio) {
        this.hotRatio = hotRatio;
    }

    public double getRandomRatio() {
        return randomRatio;
    }

    public void setRandomRatio(double randomRatio) {
        this.randomRatio = randomRatio;
    }

    public long getRandomSeed() {
        return randomSeed;
    }

    public void setRandomSeed(long randomSeed) {
        this.randomSeed = randomSeed;
    }

    public ActionWeights getActionWeights() {
        return actionWeights;
    }

    public Als getAls() {
        return als;
    }

    public static class Als {
        private boolean enabled = false;
        private String redisPrefix = "rec:user:";
        private int topn = 50;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getRedisPrefix() {
            return redisPrefix;
        }

        public void setRedisPrefix(String redisPrefix) {
            this.redisPrefix = redisPrefix;
        }

        public int getTopn() {
            return topn;
        }

        public void setTopn(int topn) {
            this.topn = topn;
        }
    }

    public static class ActionWeights {
        private int favorite = 4;
        private int comment = 3;
        private int like = 2;
        private int play = 1;

        public int getFavorite() {
            return favorite;
        }

        public void setFavorite(int favorite) {
            this.favorite = favorite;
        }

        public int getComment() {
            return comment;
        }

        public void setComment(int comment) {
            this.comment = comment;
        }

        public int getLike() {
            return like;
        }

        public void setLike(int like) {
            this.like = like;
        }

        public int getPlay() {
            return play;
        }

        public void setPlay(int play) {
            this.play = play;
        }
    }
}
