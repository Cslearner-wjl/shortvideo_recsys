// 行为事件日志写入器，负责追加 JSON Lines 到指定文件。
package com.shortvideo.recsys.backend.bigdata;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BehaviorEventLogger {
    private static final Logger log = LoggerFactory.getLogger(BehaviorEventLogger.class);
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final BehaviorLogProperties properties;
    private final ObjectMapper objectMapper;
    private final Object lock = new Object();
    private final Path path;
    private BufferedWriter writer;

    public BehaviorEventLogger(BehaviorLogProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.path = Path.of(properties.getPath());
        if (properties.isEnabled()) {
            tryInitWriter();
        }
        log.info("BehaviorEventLogger initialized: enabled={}, path={}", properties.isEnabled(), properties.getPath());
    }

    public void appendAction(String eventType, long userId, long videoId, LocalDateTime actionTime, Long durationMs, Boolean isCompleted) {
        if (!properties.isEnabled()) {
            log.debug("Behavior logging disabled, skipping event: {}", eventType);
            return;
        }

        try {
            ZonedDateTime zoned = actionTime.atZone(DEFAULT_ZONE);
            BehaviorEvent event = new BehaviorEvent(
                    UUID.randomUUID().toString(),
                    eventType,
                    userId,
                    videoId,
                    TIME_FORMATTER.format(zoned),
                    zoned.toInstant().toEpochMilli(),
                    durationMs,
                    isCompleted,
                    "backend"
            );
            String line = objectMapper.writeValueAsString(event) + System.lineSeparator();
            synchronized (lock) {
                if (writer == null) {
                    tryInitWriter();
                }
                if (writer != null) {
                    writer.write(line);
                    // Flush 每次写入以降低丢失概率，同时避免频繁 open/close 文件带来的显著开销。
                    writer.flush();
                }
            }
            log.debug("Appended behavior event: type={}, userId={}, videoId={}", eventType, userId, videoId);
        } catch (Exception ex) {
            log.warn("append behavior event failed", ex);
        }
    }

    private void tryInitWriter() {
        synchronized (lock) {
            if (writer != null) {
                return;
            }
            try {
                Path parent = path.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                writer = Files.newBufferedWriter(
                        path,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.APPEND
                );
            } catch (IOException ex) {
                // 写入器初始化失败时不中断业务流程，后续将继续尝试初始化。
                log.warn("init behavior log writer failed", ex);
                writer = null;
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        synchronized (lock) {
            if (writer == null) {
                return;
            }
            try {
                writer.flush();
                writer.close();
            } catch (IOException ex) {
                log.debug("close behavior log writer failed", ex);
            } finally {
                writer = null;
            }
        }
    }
}
