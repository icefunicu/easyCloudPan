package com.easypan.service;

/**
 * 令牌安全审计服务接口.
 */
public interface TokenSecurityAuditService {

    /**
     * 记录令牌使用情况.
     *
     * @param userId 用户 ID
     * @param token 令牌
     * @param ipAddress IP 地址
     * @param userAgent 用户代理
     */
    void recordTokenUsage(String userId, String token, String ipAddress, String userAgent);

    /**
     * 报告可疑活动.
     *
     * @param userId 用户 ID
     * @param activityType 活动类型
     * @param details 详情
     */
    void reportSuspiciousActivity(String userId, String activityType, String details);

    /**
     * 检查是否有可疑活动.
     *
     * @param userId 用户 ID
     * @param ipAddress IP 地址
     * @return 是否可疑
     */
    boolean isSuspiciousActivity(String userId, String ipAddress);
}
