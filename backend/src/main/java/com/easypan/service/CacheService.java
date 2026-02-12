package com.easypan.service;

import com.easypan.entity.po.UserInfo;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import com.easypan.mappers.UserInfoMapper;

import java.util.concurrent.TimeUnit;

/**
 * 多级缓存服务
 * 
 * 实现本地缓存 (Caffeine) → Redis → 数据库的三级缓存策略
 * 
 * 需求：2.2.1, 2.2.3
 */
@Component
@Slf4j
public class CacheService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private Cache<String, UserInfo> userInfoCache;
    
    @Autowired
    private UserInfoMapper userInfoMapper;
    
    private static final String USER_INFO_KEY = "user:info:";
    
    /**
     * 多级缓存查询用户信息
     * 
     * 查询顺序：
     * 1. 本地缓存 (Caffeine) - 最快
     * 2. Redis 缓存 - 次快
     * 3. 数据库 - 最慢
     * 
     * @param userId 用户ID
     * @return 用户信息，如果不存在返回 null
     */
    public UserInfo getUserInfo(String userId) {
        // 1. 查询本地缓存
        UserInfo userInfo = userInfoCache.getIfPresent(userId);
        if (userInfo != null) {
            log.debug("本地缓存命中: {}", userId);
            return userInfo;
        }
        
        // 2. 查询 Redis
        String redisKey = USER_INFO_KEY + userId;
        userInfo = (UserInfo) redisTemplate.opsForValue().get(redisKey);
        if (userInfo != null) {
            log.debug("Redis 缓存命中: {}", userId);
            // 更新本地缓存
            userInfoCache.put(userId, userInfo);
            return userInfo;
        }
        
        // 3. 查询数据库
        userInfo = userInfoMapper.selectByUserId(userId);
        if (userInfo != null) {
            log.debug("数据库查询: {}", userId);
            // 更新 Redis (TTL 30分钟)
            redisTemplate.opsForValue().set(redisKey, userInfo, 30, TimeUnit.MINUTES);
            // 更新本地缓存
            userInfoCache.put(userId, userInfo);
        }
        
        return userInfo;
    }
    
    /**
     * 缓存失效
     * 
     * 同时清除本地缓存和 Redis 缓存，确保数据一致性
     * 
     * @param userId 用户ID
     */
    public void invalidateUserInfo(String userId) {
        log.debug("缓存失效: {}", userId);
        // 清除本地缓存
        userInfoCache.invalidate(userId);
        // 清除 Redis 缓存
        redisTemplate.delete(USER_INFO_KEY + userId);
    }
}
