package com.easypan.service.impl;

import com.easypan.entity.po.ShareAccessLog;
import com.easypan.mappers.ShareAccessLogMapper;
import com.easypan.service.ShareAccessLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Date;

@Slf4j
@Service
public class ShareAccessLogServiceImpl implements ShareAccessLogService {

    @Resource
    private ShareAccessLogMapper shareAccessLogMapper;

    @Override
    public void logAccess(String shareId, String fileId, String visitorId,
                          String visitorIp, String userAgent, String accessType,
                          boolean success, String errorMessage) {
        try {
            ShareAccessLog accessLog = new ShareAccessLog();
            accessLog.setShareId(shareId);
            accessLog.setFileId(fileId);
            accessLog.setVisitorId(visitorId);
            accessLog.setVisitorIp(visitorIp);
            accessLog.setVisitorUserAgent(truncate(userAgent, 500));
            accessLog.setAccessType(accessType);
            accessLog.setAccessTime(new Date());
            accessLog.setSuccess(success);
            accessLog.setErrorMessage(truncate(errorMessage, 500));
            
            shareAccessLogMapper.insert(accessLog);
        } catch (Exception e) {
            log.error("[SHARE_ACCESS_LOG] Failed to log share access: shareId={}, error={}", 
                    shareId, e.getMessage(), e);
        }
    }

    @Override
    @Async
    public void logAccessAsync(String shareId, String fileId, String visitorId,
                               String visitorIp, String userAgent, String accessType,
                               boolean success, String errorMessage) {
        logAccess(shareId, fileId, visitorId, visitorIp, userAgent, accessType, success, errorMessage);
    }

    private String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength);
    }
}
