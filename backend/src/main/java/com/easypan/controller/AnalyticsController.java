package com.easypan.controller;

import com.easypan.entity.mapper.WebVitalsMetricsMapper;
import com.easypan.entity.po.WebVitalsMetrics;
import com.easypan.entity.vo.ResponseVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RestController
@RequestMapping("/analytics")
public class AnalyticsController extends ABaseController {

    private static final int MAX_METRICS_QUEUE = 1000;
    private final ConcurrentLinkedQueue<WebVitalMetric> metricsQueue = new ConcurrentLinkedQueue<>();
    private final AtomicInteger queueSize = new AtomicInteger(0);

    @Autowired(required = false)
    private WebVitalsMetricsMapper webVitalsMetricsMapper;

    @PostMapping("/web-vitals")
    public ResponseVO<Void> receiveWebVitals(@RequestBody Map<String, Object> metric) {
        try {
            WebVitalMetric vitalMetric = new WebVitalMetric();
            vitalMetric.name = (String) metric.get("name");
            vitalMetric.value = metric.get("value") instanceof Number ? 
                ((Number) metric.get("value")).doubleValue() : 0.0;
            vitalMetric.rating = (String) metric.get("rating");
            vitalMetric.page = (String) metric.get("page");
            vitalMetric.timestamp = metric.get("timestamp") instanceof Number ?
                ((Number) metric.get("timestamp")).longValue() : System.currentTimeMillis();
            vitalMetric.userAgent = (String) metric.get("userAgent");
            vitalMetric.deviceType = (String) metric.get("deviceType");
            vitalMetric.connectionType = (String) metric.get("connectionType");

            if (queueSize.get() < MAX_METRICS_QUEUE) {
                metricsQueue.offer(vitalMetric);
                queueSize.incrementAndGet();
            }

            saveMetricToDatabase(vitalMetric);

            if ("poor".equals(vitalMetric.rating)) {
                log.warn("[WebVitals] Poor performance detected: {} = {}ms on page {}", 
                    vitalMetric.name, vitalMetric.value, vitalMetric.page);
            } else {
                log.debug("[WebVitals] Received: {} = {} ({}) on page {}", 
                    vitalMetric.name, vitalMetric.value, vitalMetric.rating, vitalMetric.page);
            }

            return ResponseVO.<Void>builder()
                .status(STATUC_SUCCESS)
                .build();
        } catch (Exception e) {
            log.warn("[WebVitals] Failed to process metric: {}", e.getMessage());
            return ResponseVO.<Void>builder()
                .status(STATUC_ERROR)
                .build();
        }
    }

    private void saveMetricToDatabase(WebVitalMetric vitalMetric) {
        if (webVitalsMetricsMapper == null) return;
        
        try {
            WebVitalsMetrics entity = new WebVitalsMetrics();
            entity.setMetricName(vitalMetric.name);
            entity.setMetricValue(vitalMetric.value);
            entity.setRating(vitalMetric.rating);
            entity.setPageUrl(vitalMetric.page);
            entity.setUserAgent(vitalMetric.userAgent);
            entity.setDeviceType(vitalMetric.deviceType);
            entity.setConnectionType(vitalMetric.connectionType);
            entity.setCreatedAt(LocalDateTime.now());
            
            webVitalsMetricsMapper.insert(entity);
        } catch (Exception e) {
            log.debug("[WebVitals] Failed to save metric to database: {}", e.getMessage());
        }
    }

    @GetMapping("/web-vitals/stats")
    public ResponseVO<WebVitalsStats> getWebVitalsStats() {
        WebVitalsStats stats = new WebVitalsStats();
        
        double lcpSum = 0, inpSum = 0, clsSum = 0, fcpSum = 0, ttfbSum = 0;
        int lcpCount = 0, inpCount = 0, clsCount = 0, fcpCount = 0, ttfbCount = 0;
        int poorCount = 0;

        for (WebVitalMetric m : metricsQueue) {
            switch (m.name) {
                case "LCP": lcpSum += m.value; lcpCount++; break;
                case "INP": inpSum += m.value; inpCount++; break;
                case "CLS": clsSum += m.value; clsCount++; break;
                case "FCP": fcpSum += m.value; fcpCount++; break;
                case "TTFB": ttfbSum += m.value; ttfbCount++; break;
            }
            if ("poor".equals(m.rating)) poorCount++;
        }

        stats.totalMetrics = queueSize.get();
        stats.poorMetrics = poorCount;
        stats.avgLCP = lcpCount > 0 ? lcpSum / lcpCount : 0;
        stats.avgINP = inpCount > 0 ? inpSum / inpCount : 0;
        stats.avgCLS = clsCount > 0 ? clsSum / clsCount : 0;
        stats.avgFCP = fcpCount > 0 ? fcpSum / fcpCount : 0;
        stats.avgTTFB = ttfbCount > 0 ? ttfbSum / ttfbCount : 0;

        return ResponseVO.<WebVitalsStats>builder()
            .status(STATUC_SUCCESS)
            .data(stats)
            .build();
    }

    private static class WebVitalMetric {
        String name;
        double value;
        String rating;
        String page;
        @SuppressWarnings("unused")
        long timestamp;
        String userAgent;
        String deviceType;
        String connectionType;
    }

    @lombok.Data
    private static class WebVitalsStats {
        int totalMetrics;
        int poorMetrics;
        double avgLCP;
        double avgINP;
        double avgCLS;
        double avgFCP;
        double avgTTFB;
    }
}
