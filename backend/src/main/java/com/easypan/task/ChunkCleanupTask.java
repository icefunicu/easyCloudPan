package com.easypan.task;

import com.easypan.component.S3Component;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 分片清理定时任务.
 *
 * <p>清理过期或残留的分片文件，防止存储空间浪费
 */
@Component
@Slf4j
public class ChunkCleanupTask {

    @Autowired
    private S3Component s3Component;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String UPLOAD_PROGRESS_KEY_PREFIX = "upload:progress:";
    private static final String CHUNK_PATH_PREFIX = "chunks/";

    /**
     * 清理过期分片文件.
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredChunks() {
        log.info("[ChunkCleanup] 开始清理过期分片文件...");
        
        try {
            Set<String> keys = redisTemplate.keys(UPLOAD_PROGRESS_KEY_PREFIX + "*:completed");
            if (keys == null || keys.isEmpty()) {
                log.info("[ChunkCleanup] 没有找到需要清理的上传记录");
                return;
            }
            
            int cleanedCount = 0;
            for (String key : keys) {
                Long ttl = redisTemplate.getExpire(key, TimeUnit.HOURS);
                if (ttl != null && ttl < 0) {
                    String[] parts = key.replace(UPLOAD_PROGRESS_KEY_PREFIX, "").split(":");
                    if (parts.length >= 2) {
                        String userId = parts[0];
                        String fileMd5 = parts[1];
                        
                        cleanupChunksForFile(userId, fileMd5);
                        cleanedCount++;
                    }
                    
                    redisTemplate.delete(key);
                    redisTemplate.delete(key.replace(":completed", ":uploading"));
                }
            }
            
            log.info("[ChunkCleanup] 清理完成，共清理 {} 个过期上传会话", cleanedCount);
            
        } catch (Exception e) {
            log.error("[ChunkCleanup] 清理过期分片失败", e);
        }
    }

    private void cleanupChunksForFile(String userId, String fileMd5) {
        String chunkPathPrefix = String.format("%s%s/%s/", CHUNK_PATH_PREFIX, userId, fileMd5);
        
        try {
            s3Component.deleteDirectory(chunkPathPrefix);
            log.info("[ChunkCleanup] 清理文件分片目录: {}", chunkPathPrefix);
            
        } catch (Exception e) {
            log.warn("[ChunkCleanup] 清理文件分片失败: userId={}, fileMd5={}", userId, fileMd5, e);
        }
    }
}
