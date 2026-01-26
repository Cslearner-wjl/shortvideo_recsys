// 每日播放聚合 DTO。
package com.shortvideo.recsys.backend.admin.dto;

public class DailyPlayDto {
    private String day;
    private long playCount;

    public DailyPlayDto() {
    }

    public DailyPlayDto(String day, long playCount) {
        this.day = day;
        this.playCount = playCount;
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public long getPlayCount() {
        return playCount;
    }

    public void setPlayCount(long playCount) {
        this.playCount = playCount;
    }
}
