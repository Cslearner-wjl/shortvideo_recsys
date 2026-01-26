// 热门 TopN 聚合 DTO。
package com.shortvideo.recsys.backend.admin.dto;

public class HotTopnDto {
    private long videoId;
    private String title;
    private long playCount;
    private long likeCount;
    private long commentCount;
    private long favoriteCount;
    private double hotScore;

    public HotTopnDto() {
    }

    public HotTopnDto(long videoId, String title, long playCount, long likeCount, long commentCount, long favoriteCount, double hotScore) {
        this.videoId = videoId;
        this.title = title;
        this.playCount = playCount;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.favoriteCount = favoriteCount;
        this.hotScore = hotScore;
    }

    public long getVideoId() {
        return videoId;
    }

    public void setVideoId(long videoId) {
        this.videoId = videoId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getPlayCount() {
        return playCount;
    }

    public void setPlayCount(long playCount) {
        this.playCount = playCount;
    }

    public long getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(long likeCount) {
        this.likeCount = likeCount;
    }

    public long getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(long commentCount) {
        this.commentCount = commentCount;
    }

    public long getFavoriteCount() {
        return favoriteCount;
    }

    public void setFavoriteCount(long favoriteCount) {
        this.favoriteCount = favoriteCount;
    }

    public double getHotScore() {
        return hotScore;
    }

    public void setHotScore(double hotScore) {
        this.hotScore = hotScore;
    }
}
