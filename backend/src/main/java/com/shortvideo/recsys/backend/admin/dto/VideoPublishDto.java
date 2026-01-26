// 视频发布量统计 DTO。
package com.shortvideo.recsys.backend.admin.dto;

public class VideoPublishDto {
    private String day;
    private long publishCount;

    public VideoPublishDto() {
    }

    public VideoPublishDto(String day, long publishCount) {
        this.day = day;
        this.publishCount = publishCount;
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public long getPublishCount() {
        return publishCount;
    }

    public void setPublishCount(long publishCount) {
        this.publishCount = publishCount;
    }
}
