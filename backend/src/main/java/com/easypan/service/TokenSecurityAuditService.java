package com.easypan.service;

public interface TokenSecurityAuditService {

    void recordTokenUsage(String userId, String token, String ipAddress, String userAgent);

    void reportSuspiciousActivity(String userId, String activityType, String details);

    boolean isSuspiciousActivity(String userId, String ipAddress);
}
