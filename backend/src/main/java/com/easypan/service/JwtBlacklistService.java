package com.easypan.service;

/**
 * JWT Blacklist Service Interface
 */
public interface JwtBlacklistService {
    /**
     * Add token to blacklist
     * @param token JWT token
     * @param expirationTimeInMs Token remaining expiration time in milliseconds
     */
    void addToBlacklist(String token, long expirationTimeInMs);

    /**
     * Check if token is blacklisted
     * @param token JWT token
     * @return true if blacklisted
     */
    boolean isBlacklisted(String token);
}
