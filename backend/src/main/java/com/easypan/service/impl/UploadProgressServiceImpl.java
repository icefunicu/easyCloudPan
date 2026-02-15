package com.easypan.service.impl;

import com.easypan.component.RedisUtils;
import com.easypan.entity.constants.Constants;
import com.easypan.entity.dto.UploadProgressDto;
import com.easypan.service.UploadProgressService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * 上传进度服务实现.
 */
@Service("uploadProgressService")
public class UploadProgressServiceImpl implements UploadProgressService {

    @Resource
    private RedisUtils<Object> redisUtils;

    private static final String PROGRESS_KEY_PREFIX = "upload:progress:";

    @Override
    public void updateProgress(String userId, String fileId, Integer chunkIndex, Integer totalChunks) {
        String key = getProgressKey(userId, fileId);
        // We use a simple counter for uploaded chunks.
        // Note: this assumes successful upload of chunk.
        // For more complex tracking (which chunk is missing), we'd need a BitMap.
        // Here we just increment a counter.
        // But since chunks can be re-uploaded, incr might be inaccurate if not careful.
        // Ideally, we'd use a BitSet or Set to track unique chunks.
        // Redis Set is good.
        redisUtils.setSet(key, chunkIndex);
        redisUtils.expire(key, Constants.REDIS_KEY_EXPIRES_ONE_HOUR); // 1 hour expiry
    }

    @Override
    public UploadProgressDto getProgress(String userId, String fileId) {
        String key = getProgressKey(userId, fileId);
        long uploadedCount = redisUtils.getSetSize(key);

        // We don't store totalChunks in Redis usually, unless passed.
        // But the frontend usually knows the totalChunks or we can store it on first
        // chunk.
        // For this simple implementation, we just return the count.
        return UploadProgressDto.builder()
                .completedChunks((int) uploadedCount)
                .build();
    }

    @Override
    public void clearProgress(String userId, String fileId) {
        redisUtils.delete(getProgressKey(userId, fileId));
    }

    private String getProgressKey(String userId, String fileId) {
        return PROGRESS_KEY_PREFIX + userId + ":" + fileId;
    }
}
