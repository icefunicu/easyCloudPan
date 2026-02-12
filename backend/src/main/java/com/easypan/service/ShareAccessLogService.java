package com.easypan.service;

public interface ShareAccessLogService {

    void logAccess(String shareId, String fileId, String visitorId, 
                   String visitorIp, String userAgent, String accessType, 
                   boolean success, String errorMessage);

    void logAccessAsync(String shareId, String fileId, String visitorId,
                        String visitorIp, String userAgent, String accessType,
                        boolean success, String errorMessage);
}
