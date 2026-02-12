package com.easypan.service.impl;

import com.easypan.component.RedisComponent;
import com.easypan.service.JwtBlacklistService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

@Service("jwtBlacklistService")
public class JwtBlacklistServiceImpl implements JwtBlacklistService {

    @Resource
    private RedisComponent redisComponent;

    @Override
    public void addToBlacklist(String token, long expirationTimeInMs) {
        // Convert ms to seconds
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
