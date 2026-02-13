package com.easypan.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.easypan.entity.dto.SysSettingsDto;
import com.easypan.entity.po.UserInfo;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine 本地缓存配置.
 *
 * <p>实现多级缓存策略的本地缓存层：
 * 用户信息缓存：5000 条，10 分钟过期；
 * 系统设置缓存：100 条，30 分钟过期.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 默认缓存管理器.
     *
     * @return 缓存管理器
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats());
        return cacheManager;
    }

    /**
     * 用户信息缓存.
     * 最大 5000 条，10 分钟过期.
     *
     * @return 用户信息缓存
     */
    @Bean
    public Cache<String, UserInfo> userInfoCache() {
        return Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    /**
     * 系统设置缓存.
     * 最大 100 条，30 分钟过期.
     *
     * @return 系统设置缓存
     */
    @Bean
    public Cache<String, SysSettingsDto> sysSettingsCache() {
        return Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }
}
