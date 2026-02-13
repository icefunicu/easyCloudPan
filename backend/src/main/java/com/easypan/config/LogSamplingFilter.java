package com.easypan.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.Marker;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 日志采样过滤器.
 * 用于减少重复日志输出，提高日志可读性.
 */
public class LogSamplingFilter extends TurboFilter {

    private double sampleRate = 0.1;
    private int deduplicationWindowSeconds = 60;
    private int maxDuplicatesPerWindow = 5;

    private final Map<String, LogEntry> recentLogs = new ConcurrentHashMap<>();
    private final AtomicInteger totalSampled = new AtomicInteger(0);
    private final AtomicInteger totalDropped = new AtomicInteger(0);

    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {
        if (format == null) {
            return FilterReply.NEUTRAL;
        }

        if (level.isGreaterOrEqual(Level.WARN)) {
            return FilterReply.NEUTRAL;
        }

        String logKey = generateLogKey(logger.getName(), format, level);

        LogEntry entry = recentLogs.compute(logKey, (key, existing) -> {
            long now = System.currentTimeMillis();
            if (existing == null) {
                return new LogEntry(now, 1);
            }
            if (now - existing.firstSeen > deduplicationWindowSeconds * 1000L) {
                return new LogEntry(now, 1);
            }
            existing.count++;
            return existing;
        });

        if (entry.count > maxDuplicatesPerWindow) {
            totalDropped.incrementAndGet();
            return FilterReply.DENY;
        }

        if (Math.random() > sampleRate) {
            totalDropped.incrementAndGet();
            return FilterReply.DENY;
        }

        totalSampled.incrementAndGet();
        return FilterReply.NEUTRAL;
    }

    private String generateLogKey(String loggerName, String format, Level level) {
        return loggerName + ":" + level + ":" + format.hashCode();
    }

    /**
     * 设置采样率.
     *
     * @param sampleRate 采样率（0.0-1.0）
     */
    public void setSampleRate(double sampleRate) {
        this.sampleRate = Math.max(0.0, Math.min(1.0, sampleRate));
    }

    /**
     * 设置去重窗口时间（秒）.
     *
     * @param deduplicationWindowSeconds 窗口时间
     */
    public void setDeduplicationWindowSeconds(int deduplicationWindowSeconds) {
        this.deduplicationWindowSeconds = deduplicationWindowSeconds;
    }

    /**
     * 设置窗口内最大重复数.
     *
     * @param maxDuplicatesPerWindow 最大重复数
     */
    public void setMaxDuplicatesPerWindow(int maxDuplicatesPerWindow) {
        this.maxDuplicatesPerWindow = maxDuplicatesPerWindow;
    }

    /**
     * 获取统计信息.
     *
     * @return 统计信息
     */
    public Map<String, Object> getStats() {
        return Map.of(
                "totalSampled", totalSampled.get(),
                "totalDropped", totalDropped.get(),
                "sampleRate", sampleRate,
                "uniqueLogKeys", recentLogs.size()
        );
    }

    private static class LogEntry {
        long firstSeen;
        int count;

        LogEntry(long firstSeen, int count) {
            this.firstSeen = firstSeen;
            this.count = count;
        }
    }
}
