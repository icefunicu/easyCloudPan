package com.easypan.service;

import com.easypan.entity.po.UserInfo;
import com.github.benmanes.caffeine.cache.Cache;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import com.easypan.mappers.UserInfoMapper;

import java.util.concurrent.TimeUnit;

import static com.easypan.entity.po.table.UserInfoTableDef.USER_INFO;

/**
 * 缓存服务，提供多级缓存支持.
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
     * 获取用户信息（多级缓存）.
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    public UserInfo getUserInfo(String userId) {
        UserInfo userInfo = userInfoCache.getIfPresent(userId);
        if (userInfo != null) {
            log.debug("本地缓存命中: {}", userId);
            return userInfo;
        }
        
        String redisKey = USER_INFO_KEY + userId;
        userInfo = (UserInfo) redisTemplate.opsForValue().get(redisKey);
        if (userInfo != null) {
            log.debug("Redis 缓存命中: {}", userId);
            userInfoCache.put(userId, userInfo);
            return userInfo;
        }
        
        userInfo = userInfoMapper.selectOneByQuery(
                QueryWrapper.create().where(USER_INFO.USER_ID.eq(userId)));
        if (userInfo != null) {
            log.debug("数据库查询: {}", userId);
            redisTemplate.opsForValue().set(redisKey, userInfo, 30, TimeUnit.MINUTES);
            userInfoCache.put(userId, userInfo);
        }
        
        return userInfo;
    }
    
    /**
     * 使缓存失效.
     *
     * @param userId 用户ID
     */
    public void invalidateUserInfo(String userId) {
        log.debug("缓存失效: {}", userId);
        userInfoCache.invalidate(userId);
        redisTemplate.delete(USER_INFO_KEY + userId);
    }
}
