// 活跃用户聚合 DTO。
package com.shortvideo.recsys.backend.admin.dto;

public class ActiveUserDto {
    private String day;
    private long activeUserCount;

    public ActiveUserDto() {
    }

    public ActiveUserDto(String day, long activeUserCount) {
        this.day = day;
        this.activeUserCount = activeUserCount;
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public long getActiveUserCount() {
        return activeUserCount;
    }

    public void setActiveUserCount(long activeUserCount) {
        this.activeUserCount = activeUserCount;
    }
}
