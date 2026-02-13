package com.easypan.config;

import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 * 文件上传配置.
 * 提供并发控制和速率限制.
 */
@Configuration
public class UploadConfig {

    private static final int MAX_CONCURRENT_UPLOADS_PER_USER = 3;

    private final ConcurrentHashMap<String, Semaphore> userUploadSemaphores = new ConcurrentHashMap<>();

    /**
     * 获取用户上传信号量.
     * 如果不存在则创建.
     *
     * @param userId 用户ID
     * @return 用户专属的上传信号量
     */
    public Semaphore getUserUploadSemaphore(String userId) {
        return userUploadSemaphores.computeIfAbsent(userId,
                k -> new Semaphore(MAX_CONCURRENT_UPLOADS_PER_USER, true));
    }

    /**
     * 清理用户上传信号量（用户登出或长时间未活动时调用）.
     *
     * @param userId 用户ID
     */
    public void clearUserUploadSemaphore(String userId) {
        userUploadSemaphores.remove(userId);
    }

    /**
     * 获取用户当前可用上传槽位数.
     *
     * @param userId 用户ID
     * @return 可用槽位数
     */
    public int getAvailableUploadSlots(String userId) {
        Semaphore semaphore = userUploadSemaphores.get(userId);
        return semaphore != null ? semaphore.availablePermits() : MAX_CONCURRENT_UPLOADS_PER_USER;
    }
}
