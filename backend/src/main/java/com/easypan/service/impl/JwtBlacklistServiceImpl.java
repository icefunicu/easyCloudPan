package com.easypan.service.impl;

import com.easypan.component.RedisComponent;
import com.easypan.service.JwtBlacklistService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

/**
 * JWT 黑名单服务实现类.
 */
@Service("jwtBlacklistService")
public class JwtBlacklistServiceImpl implements JwtBlacklistService {

    @Resource
    private RedisComponent redisComponent;

    @Override
    public void addToBlacklist(String token, long expirationTimeInMs) {
        long seconds = expirationTimeInMs / 1000;
        if (seconds > 0) {
            redisComponent.addBlacklistToken(token, seconds);
        }
    }

    @Override
    public boolean isBlacklisted(String token) {
        return redisComponent.isTokenBlacklisted(token);
    }
}
