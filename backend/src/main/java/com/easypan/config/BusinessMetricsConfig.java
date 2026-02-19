package com.easypan.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 业务指标监控配置.
 * 定义关键业务指标，用于 Prometheus 采集和 Grafana 展示.
 */
@Configuration
public class BusinessMetricsConfig {

    public static final String METRIC_PREFIX = "easypan_";

    // ========== 上传相关指标 ==========

    /**
     * 文件上传成功计数器.
     */
    @Bean
    public Counter uploadSuccessCounter(MeterRegistry registry) {
        return Counter.builder(METRIC_PREFIX + "upload_total")
                .tag("status", "success")
                .description("Total number of successful file uploads")
                .register(registry);
    }

    /**
     * 文件上传失败计数器.
     */
    @Bean
    public Counter uploadFailureCounter(MeterRegistry registry) {
        return Counter.builder(METRIC_PREFIX + "upload_total")
                .tag("status", "failure")
                .description("Total number of failed file uploads")
                .register(registry);
    }

    /**
     * 秒传成功计数器.
     */
    @Bean
    public Counter instantUploadCounter(MeterRegistry registry) {
        return Counter.builder(METRIC_PREFIX + "instant_upload_total")
                .description("Total number of instant uploads (MD5 deduplication)")
                .register(registry);
    }

    /**
     * 上传文件大小总计（字节）.
     */
    @Bean
    public AtomicLong uploadBytesTotal(MeterRegistry registry) {
        AtomicLong bytes = new AtomicLong(0);
        Gauge.builder(METRIC_PREFIX + "upload_bytes_total", bytes, AtomicLong::get)
                .description("Total bytes uploaded")
                .register(registry);
        return bytes;
    }

    /**
     * 上传耗时监控.
     */
    @Bean
    public Timer uploadTimer(MeterRegistry registry) {
        return Timer.builder(METRIC_PREFIX + "upload_duration")
                .description("File upload duration")
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .register(registry);
    }

    // ========== 下载相关指标 ==========

    /**
     * 文件下载计数器.
     */
    @Bean
    public Counter downloadCounter(MeterRegistry registry) {
        return Counter.builder(METRIC_PREFIX + "download_total")
                .description("Total number of file downloads")
                .register(registry);
    }

    /**
     * 下载文件大小总计（字节）.
     */
    @Bean
    public AtomicLong downloadBytesTotal(MeterRegistry registry) {
        AtomicLong bytes = new AtomicLong(0);
        Gauge.builder(METRIC_PREFIX + "download_bytes_total", bytes, AtomicLong::get)
                .description("Total bytes downloaded")
                .register(registry);
        return bytes;
    }

    // ========== 分享相关指标 ==========

    /**
     * 分享创建计数器.
     */
    @Bean
    public Counter shareCreatedCounter(MeterRegistry registry) {
        return Counter.builder(METRIC_PREFIX + "share_created_total")
                .description("Total number of shares created")
                .register(registry);
    }

    /**
     * 分享访问计数器.
     */
    @Bean
    public Counter shareAccessCounter(MeterRegistry registry) {
        return Counter.builder(METRIC_PREFIX + "share_access_total")
                .description("Total number of share accesses")
                .register(registry);
    }

    /**
     * 分享保存计数器.
     */
    @Bean
    public Counter shareSaveCounter(MeterRegistry registry) {
        return Counter.builder(METRIC_PREFIX + "share_save_total")
                .description("Total number of shares saved to user's folder")
                .register(registry);
    }

    // ========== 用户相关指标 ==========

    /**
     * 活跃用户数.
     */
    @Bean
    public AtomicLong activeUsersGauge(MeterRegistry registry) {
        AtomicLong users = new AtomicLong(0);
        Gauge.builder(METRIC_PREFIX + "active_users", users, AtomicLong::get)
                .description("Number of active users")
                .register(registry);
        return users;
    }

    /**
     * 用户注册计数器.
     */
    @Bean
    public Counter registrationCounter(MeterRegistry registry) {
        return Counter.builder(METRIC_PREFIX + "registration_total")
                .description("Total number of user registrations")
                .register(registry);
    }

    /**
     * 登录成功计数器.
     */
    @Bean
    public Counter loginSuccessCounter(MeterRegistry registry) {
        return Counter.builder(METRIC_PREFIX + "login_total")
                .tag("status", "success")
                .description("Total number of successful logins")
                .register(registry);
    }

    /**
     * 登录失败计数器.
     */
    @Bean
    public Counter loginFailureCounter(MeterRegistry registry) {
        return Counter.builder(METRIC_PREFIX + "login_total")
                .tag("status", "failure")
                .description("Total number of failed logins")
                .register(registry);
    }

    // ========== 存储相关指标 ==========

    /**
     * 存储使用量（字节）.
     */
    @Bean
    public AtomicLong storageUsedGauge(MeterRegistry registry) {
        AtomicLong storage = new AtomicLong(0);
        Gauge.builder(METRIC_PREFIX + "storage_used_bytes", storage, AtomicLong::get)
                .description("Total storage used in bytes")
                .register(registry);
        return storage;
    }

    /**
     * 文件总数.
     */
    @Bean
    public AtomicLong totalFilesGauge(MeterRegistry registry) {
        AtomicLong files = new AtomicLong(0);
        Gauge.builder(METRIC_PREFIX + "files_total", files, AtomicLong::get)
                .description("Total number of files stored")
                .register(registry);
        return files;
    }

    // ========== 缓存相关指标 ==========

    /**
     * 缓存命中计数器.
     */
    @Bean
    public Counter cacheHitCounter(MeterRegistry registry) {
        return Counter.builder(METRIC_PREFIX + "cache_requests_total")
                .tag("result", "hit")
                .description("Total number of cache hits")
                .register(registry);
    }

    /**
     * 缓存未命中计数器.
     */
    @Bean
    public Counter cacheMissCounter(MeterRegistry registry) {
        return Counter.builder(METRIC_PREFIX + "cache_requests_total")
                .tag("result", "miss")
                .description("Total number of cache misses")
                .register(registry);
    }

    // ========== API 性能指标 ==========

    /**
     * API 请求计数器.
     */
    @Bean
    public Counter apiRequestCounter(MeterRegistry registry) {
        return Counter.builder(METRIC_PREFIX + "api_requests_total")
                .description("Total number of API requests")
                .register(registry);
    }

    /**
     * API 错误计数器.
     */
    @Bean
    public Counter apiErrorCounter(MeterRegistry registry) {
        return Counter.builder(METRIC_PREFIX + "api_errors_total")
                .description("Total number of API errors")
                .register(registry);
    }
}
