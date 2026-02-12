package com.easypan.component;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT Token 提供者
 * 用于生成、解析和验证 JWT Token
 */
@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${jwt.secret:defaultSecretKeyRequires256BitsMinimumLengthForHS256AlgorithmIsHere}")
    private String jwtSecret;

    @Value("${jwt.expiration:604800000}") // 7 days
    private long jwtExpirationInMs;

    @Value("${jwt.refresh-expiration:2592000000}") // 30 days
    private long jwtRefreshExpirationInMs;

    @org.springframework.beans.factory.annotation.Autowired
    private com.easypan.service.JwtBlacklistService jwtBlacklistService;

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成 Token
     */
    public String generateToken(String userId, Map<String, Object> claims) {
        return generateToken(userId, claims, jwtExpirationInMs);
    }

    /**
     * 生成 Refresh Token
     */
    public String generateRefreshToken(String userId, Map<String, Object> claims) {
        return generateToken(userId, claims, jwtRefreshExpirationInMs);
    }

    private String generateToken(String userId, Map<String, Object> claims, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        JwtBuilder builder = Jwts.builder()
                .subject(userId)
                .issuedAt(new Date())
                .expiration(expiryDate)
                .signWith(getSigningKey());
        
        if (claims != null) {
            builder.claims(claims);
        }

        String token = builder.compact();
        logger.info("JWT token generated for userId={}, expiresAt={}", userId, expiryDate);
        return token;
    }

    /**
     * 从 Token 中获取 User ID
     */
    public String getUserIdFromJWT(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    /**
     * 获取 Token 剩余有效期（毫秒）
     */
    public long getRemainingTime(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            Date expiration = claims.getExpiration();
            return expiration.getTime() - System.currentTimeMillis();
        } catch (Exception ex) {
            return 0;
        }
    }

    /**
     * 验证 Token 是否有效
     */
    public boolean validateToken(String authToken) {
        try {
            if (jwtBlacklistService.isBlacklisted(authToken)) {
                logger.warn("JWT Token is in blacklist: {}", authToken);
                return false;
            }

            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(authToken);
            return true;
        } catch (MalformedJwtException ex) {
            logger.warn("Invalid JWT token", ex);
        } catch (ExpiredJwtException ex) {
            logger.info("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            logger.warn("Unsupported JWT token", ex);
        } catch (IllegalArgumentException ex) {
            logger.warn("JWT claims string is empty.", ex);
        } catch (Exception ex) {
            logger.error("JWT validation error", ex);
        }
        return false;
    }
}
