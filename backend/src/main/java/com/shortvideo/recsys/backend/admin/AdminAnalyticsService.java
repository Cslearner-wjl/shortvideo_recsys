// 管理端看板服务，负责参数校验与聚合查询。
package com.shortvideo.recsys.backend.admin;

import com.shortvideo.recsys.backend.admin.dto.ActiveUserDto;
import com.shortvideo.recsys.backend.admin.dto.DailyPlayDto;
import com.shortvideo.recsys.backend.admin.dto.HotTopnDto;
import com.shortvideo.recsys.backend.admin.dto.UserGrowthDto;
import com.shortvideo.recsys.backend.admin.dto.VideoPublishDto;
import com.shortvideo.recsys.backend.common.BizException;
import com.shortvideo.recsys.backend.common.ErrorCodes;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile({"docker", "test"})
public class AdminAnalyticsService {
    private final AdminAnalyticsMapper adminAnalyticsMapper;

    public AdminAnalyticsService(AdminAnalyticsMapper adminAnalyticsMapper) {
        this.adminAnalyticsMapper = adminAnalyticsMapper;
    }

    public List<DailyPlayDto> dailyPlay(String from, String to) {
        DateRange range = parseRange(from, to);
        return adminAnalyticsMapper.selectDailyPlay(range.fromStart(), range.toEndExclusive());
    }

    public List<UserGrowthDto> userGrowth(String from, String to) {
        DateRange range = parseRange(from, to);
        return adminAnalyticsMapper.selectUserGrowth(range.fromStart(), range.toEndExclusive());
    }

    public List<ActiveUserDto> activeUsers(String from, String to) {
        DateRange range = parseRange(from, to);
        return adminAnalyticsMapper.selectActiveUsers(range.fromStart(), range.toEndExclusive());
    }

    public List<VideoPublishDto> videoPublish(String from, String to) {
        DateRange range = parseRange(from, to);
        return adminAnalyticsMapper.selectVideoPublish(range.fromStart(), range.toEndExclusive());
    }

    public List<HotTopnDto> hotTopn(Integer n) {
        int limit = n == null || n <= 0 ? 10 : Math.min(n, 100);
        return adminAnalyticsMapper.selectHotTopn(limit);
    }

    private DateRange parseRange(String from, String to) {
        if (from == null || to == null) {
            throw new BizException(ErrorCodes.BAD_REQUEST, "参数错误");
        }
        LocalDate fromDate;
        LocalDate toDate;
        try {
            fromDate = LocalDate.parse(from);
            toDate = LocalDate.parse(to);
        } catch (DateTimeParseException ex) {
            throw new BizException(ErrorCodes.BAD_REQUEST, "参数错误");
        }
        if (toDate.isBefore(fromDate)) {
            throw new BizException(ErrorCodes.BAD_REQUEST, "参数错误");
        }
        LocalDateTime fromStart = fromDate.atStartOfDay();
        LocalDateTime toEndExclusive = toDate.plusDays(1).atStartOfDay();
        return new DateRange(fromStart, toEndExclusive);
    }

    private record DateRange(LocalDateTime fromStart, LocalDateTime toEndExclusive) {
    }
}
