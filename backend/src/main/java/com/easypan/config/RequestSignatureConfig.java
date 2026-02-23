package com.easypan.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 请求签名验证过滤器.
 * 防止重放攻击，确保请求的完整性和唯一性.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestSignatureConfig extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RequestSignatureConfig.class);

    private static final String SIGNATURE_HEADER = "X-Signature";
    private static final String TIMESTAMP_HEADER = "X-Timestamp";
    private static final String NONCE_HEADER = "X-Nonce";
    private static final String APP_KEY_HEADER = "X-App-Key";

    // 签名有效期（毫秒），默认 5 分钟
    private static final long SIGNATURE_VALIDITY_MS = 5 * 60 * 1000;

    // 非ce缓存前缀
    private static final String NONCE_CACHE_PREFIX = "easypan:nonce:";

    // 跳过签名验证的路径
    private static final Set<String> SKIP_PATHS = Set.of(
            "/api/login",
            "/api/register",
            "/api/logout",
            "/api/refreshToken",
            "/api/getImageCode",
            "/api/sendEmailCode",
            "/api/actuator",
            "/api/swagger-ui",
            "/api/v3/api-docs"
    );

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public RequestSignatureConfig(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // 跳过 OPTIONS 请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestPath = request.getRequestURI();

        // 跳过不需要签名验证的路径
        if (shouldSkipSignature(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        String signature = request.getHeader(SIGNATURE_HEADER);
        String timestamp = request.getHeader(TIMESTAMP_HEADER);
        String nonce = request.getHeader(NONCE_HEADER);

        // 如果没有签名头，跳过验证（兼容现有客户端）
        if (signature == null || timestamp == null || nonce == null) {
            logger.debug("Request without signature headers, skipping validation: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        // 验证时间戳
        if (!validateTimestamp(timestamp)) {
            logger.warn("Request timestamp expired: path={}, timestamp={}", requestPath, timestamp);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "请求已过期");
            return;
        }

        // 验证 nonce（防止重放）
        if (!validateNonce(nonce)) {
            logger.warn("Request nonce already used: path={}, nonce={}", requestPath, nonce);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "重复的请求");
            return;
        }

        // 验证签名
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        filterChain.doFilter(wrappedRequest, response);

        // 缓存 nonce
        cacheNonce(nonce);
    }

    private boolean shouldSkipSignature(String path) {
        return SKIP_PATHS.stream().anyMatch(path::startsWith);
    }

    private boolean validateTimestamp(String timestamp) {
        try {
            long requestTime = Long.parseLong(timestamp);
            long currentTime = System.currentTimeMillis();
            return Math.abs(currentTime - requestTime) <= SIGNATURE_VALIDITY_MS;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean validateNonce(String nonce) {
        String key = NONCE_CACHE_PREFIX + nonce;
        return !Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    private void cacheNonce(String nonce) {
        String key = NONCE_CACHE_PREFIX + nonce;
        redisTemplate.opsForValue().set(key, "1", SIGNATURE_VALIDITY_MS, TimeUnit.MILLISECONDS);
    }

    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> error = new HashMap<>();
        error.put("status", "error");
        error.put("code", status);
        error.put("info", message);
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }

    /**
     * 生成签名.
     * 签名算法：HMAC-SHA256(timestamp + nonce + method + path + body, secret)
     *
     * @param timestamp 时间戳
     * @param nonce     随机字符串
     * @param method    HTTP 方法
     * @param path      请求路径
     * @param body      请求体
     * @param secret    密钥
     * @return 签名字符串
     */
    public static String generateSignature(
            String timestamp,
            String nonce,
            String method,
            String path,
            String body,
            String secret) {
        try {
            String data = String.join("&", timestamp, nonce, method, path, body == null ? "" : body);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String input = data + secret;
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate signature", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
