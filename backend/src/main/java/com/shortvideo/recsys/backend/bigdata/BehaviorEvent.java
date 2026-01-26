// 行为事件结构，用于日志与 Kafka 链路的统一 schema。
package com.shortvideo.recsys.backend.bigdata;

public record BehaviorEvent(
        String eventId,
        String eventType,
        long userId,
        long videoId,
        String actionTime,
        long actionTs,
        Long durationMs,
        Boolean isCompleted,
        String source
) {
}
