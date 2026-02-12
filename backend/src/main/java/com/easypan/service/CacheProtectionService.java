package com.easypan.service;

import com.easypan.component.RedisUtils;
import com.easypan.entity.po.UserInfo;
import com.easypan.mappers.UserInfoMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 缓存保护服务
 * 实现缓存穿透、击穿、雪崩防护机制
 */
@Component
public class CacheProtectionService {

    private static final Logger log = LoggerFactory.getLogger(CacheProtectionService.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedisUtils<Object> redisUtils;

    @Autowired
    private UserInfoMapper userInfoMapper;

    private static final String NULL_VALUE = "NULL";
    private static final long NULL_TTL = 2; // 2分钟

    /**
     * 防止缓存穿透 - 缓存空值
     * 当查询的数据不存在时，缓存一个空值标记，避免每次都查询数据库
     *
     * @param userId 用户ID
     * @return 用户信息，不存在返回null
     */
    public UserInfo getUserInfoWithNullCache(String userId) {
        String key = "user:info:" + userId;
        Object cached = redisUtils.get(key);

        // 检查是否是缓存的空值
        if (NULL_VALUE.equals(cached)) {
            log.debug("缓存空值命中: {}", userId);
            return null;
        }

        // 缓存命中
        if (cached != null) {
            log.debug("缓存命中: {}", userId);
            return (UserInfo) cached;
        }

        // 查询数据库
        UserInfo userInfo = userInfoMapper.selectByUserId(userId);
        if (userInfo == null) {
            // 缓存空值，防止穿透
            log.debug("缓存空值以防止穿透: {}", userId);
            redisUtils.setex(key, NULL_VALUE, NULL_TTL * 60);
        } else {
            // 缓存正常数据
            redisUtils.setex(key, userInfo, 30 * 60); // 30分钟
        }

        return userInfo;
    }

    /**
     * 防止缓存击穿 - 分布式锁
     * 当热点数据过期时，使用分布式锁确保只有一个请求去查询数据库
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    public UserInfo getUserInfoWithLock(String userId) {
        String key = "user:info:" + userId;
        String lockKey = "lock:" + key;

        // 先尝试从缓存获取
        Object cached = redisUtils.get(key);
        if (cached != null && !NULL_VALUE.equals(cached)) {
            log.debug("缓存命中: {}", userId);
            return (UserInfo) cached;
        }

        // 尝试获取分布式锁
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(
                lockKey, "1", 10, TimeUnit.SECONDS);

        if (Boolean.TRUE.equals(locked)) {
            try {
                // 双重检查，避免重复查询
                cached = redisUtils.get(key);
                if (cached != null && !NULL_VALUE.equals(cached)) {
                    return (UserInfo) cached;
                }

                // 查询数据库
                log.debug("获取锁成功，查询数据库: {}", userId);
                UserInfo userInfo = userInfoMapper.selectByUserId(userId);
                if (userInfo != null) {
                    // 使用随机TTL防止雪崩
                    setWithRandomTTL(key, userInfo, 30 * 60);
                } else {
                    // 缓存空值防止穿透
                    redisUtils.setex(key, NULL_VALUE, NULL_TTL * 60);
                }
                return userInfo;
            } finally {
                // 释放锁
                redisTemplate.delete(lockKey);
            }
        } else {
            // 未获取到锁，等待后重试
            try {
                log.debug("等待锁释放: {}", userId);
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("等待锁时被中断: {}", userId);
            }
            return getUserInfoWithLock(userId);
        }
    }

    /**
     * 防止缓存雪崩 - 随机 TTL
     * 在基础TTL上增加随机时间，避免大量缓存同时过期
     *
     * @param key     缓存键
     * @param value   缓存值
     * @param baseTTL 基础TTL（秒）
     */
    public void setWithRandomTTL(String key, Object value, long baseTTL) {
        // 在基础TTL上增加0-300秒的随机时间
        long randomTTL = baseTTL + ThreadLocalRandom.current().nextLong(300);
        redisUtils.setex(key, value, randomTTL);
        log.debug("设置随机TTL缓存: key={}, ttl={}秒", key, randomTTL);
    }

    /**
     * 综合防护方法
     * 同时应用穿透、击穿、雪崩防护
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    public UserInfo getUserInfoWithFullProtection(String userId) {
        String key = "user:info:" + userId;
        String lockKey = "lock:" + key;

        // 1. 先尝试从缓存获取
        Object cached = redisUtils.get(key);
        
        // 检查空值缓存（防穿透）
        if (NULL_VALUE.equals(cached)) {
            log.debug("缓存空值命中: {}", userId);
            return null;
        }
        
        if (cached != null) {
            log.debug("缓存命中: {}", userId);
            return (UserInfo) cached;
        }

        // 2. 使用分布式锁（防击穿）
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(
                lockKey, "1", 10, TimeUnit.SECONDS);

        if (Boolean.TRUE.equals(locked)) {
            try {
                // 双重检查
                cached = redisUtils.get(key);
                if (cached != null) {
                    if (NULL_VALUE.equals(cached)) {
                        return null;
                    }
                    return (UserInfo) cached;
                }

                // 3. 查询数据库
                log.debug("获取锁成功，查询数据库: {}", userId);
                UserInfo userInfo = userInfoMapper.selectByUserId(userId);
                
                if (userInfo != null) {
                    // 使用随机TTL（防雪崩）
                    setWithRandomTTL(key, userInfo, 30 * 60);
                } else {
                    // 缓存空值（防穿透）
                    redisUtils.setex(key, NULL_VALUE, NULL_TTL * 60);
                }
                
                return userInfo;
            } finally {
                redisTemplate.delete(lockKey);
            }
        } else {
            // 未获取到锁，等待后重试
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return getUserInfoWithFullProtection(userId);
        }
    }

    /**
     * 清除缓存
     *
     * @param userId 用户ID
     */
    public void invalidateUserInfo(String userId) {
        String key = "user:info:" + userId;
        redisUtils.delete(key);
        log.debug("清除缓存: {}", key);
    }
}
