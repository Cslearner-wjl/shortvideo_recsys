// 用户增长聚合 DTO。
package com.shortvideo.recsys.backend.admin.dto;

public class UserGrowthDto {
    private String day;
    private long newUserCount;

    public UserGrowthDto() {
    }

    public UserGrowthDto(String day, long newUserCount) {
        this.day = day;
        this.newUserCount = newUserCount;
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public long getNewUserCount() {
        return newUserCount;
    }

    public void setNewUserCount(long newUserCount) {
        this.newUserCount = newUserCount;
    }
}
