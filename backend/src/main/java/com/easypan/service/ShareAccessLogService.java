package com.easypan.service;

/**
 * 分享访问日志服务接口.
 */
public interface ShareAccessLogService {

    /**
     * 记录访问日志.
     *
     * @param shareId 分享 ID
     * @param fileId 文件 ID
     * @param visitorId 访问者 ID
     * @param visitorIp 访问者 IP
     * @param userAgent 用户代理
     * @param accessType 访问类型
     * @param success 是否成功
     * @param errorMessage 错误信息
     */
    void logAccess(String shareId, String fileId, String visitorId,
            String visitorIp, String userAgent, String accessType,
            boolean success, String errorMessage);

    /**
     * 异步记录访问日志.
     *
     * @param shareId 分享 ID
     * @param fileId 文件 ID
     * @param visitorId 访问者 ID
     * @param visitorIp 访问者 IP
     * @param userAgent 用户代理
     * @param accessType 访问类型
     * @param success 是否成功
     * @param errorMessage 错误信息
     */
    void logAccessAsync(String shareId, String fileId, String visitorId,
            String visitorIp, String userAgent, String accessType,
            boolean success, String errorMessage);
}
