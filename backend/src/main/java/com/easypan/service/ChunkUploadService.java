package com.easypan.service;

import com.easypan.component.S3Component;
import com.easypan.entity.dto.UploadResultDto;
import com.easypan.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 分片上传服务.
 *
 * <p>实现分片上传并发控制，限制同一文件最大并发数为 5
 *
 * <p>需求：2.3.1
 */
@Service
@Slf4j
public class ChunkUploadService {

    @Autowired
    private S3Component s3Component;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 同一文件最大并发上传分片数.
     */
    private static final int MAX_CONCURRENT_CHUNKS = 5;

    /**
     * Redis key 前缀：记录上传中的分片.
     */
    private static final String UPLOAD_PROGRESS_KEY = "upload:progress:";

    /**
     * 分片上传（带并发控制）.
     *
     * @param userId 用户ID
     * @param fileMd5 文件MD5值
     * @param chunkIndex 分片索引
     * @param totalChunks 总分片数
     * @param chunkFile 分片文件
     * @return 上传结果
     * @throws IOException IO异常
     */
    public UploadResultDto uploadChunk(
            String userId,
            String fileMd5,
            Integer chunkIndex,
            Integer totalChunks,
            MultipartFile chunkFile) throws IOException {

        String uploadKey = UPLOAD_PROGRESS_KEY + userId + ":" + fileMd5;

        Long currentConcurrent = redisTemplate.opsForSet().size(uploadKey + ":uploading");
        if (currentConcurrent != null && currentConcurrent >= MAX_CONCURRENT_CHUNKS) {
            log.warn("并发上传数超限: userId={}, fileMd5={}, current={}", userId, fileMd5, currentConcurrent);
            throw new BusinessException("并发上传数超限，请稍后重试");
        }

        redisTemplate.opsForSet().add(uploadKey + ":uploading", chunkIndex);
        redisTemplate.expire(uploadKey + ":uploading", 1, TimeUnit.HOURS);

        try {
            String chunkPath = String.format("chunks/%s/%s/%d", userId, fileMd5, chunkIndex);
            s3Component.uploadBytes(chunkPath, chunkFile.getBytes());

            log.info("分片上传成功: userId={}, fileMd5={}, chunkIndex={}/{}",
                    userId, fileMd5, chunkIndex, totalChunks);

            redisTemplate.opsForSet().add(uploadKey + ":completed", chunkIndex);
            redisTemplate.expire(uploadKey + ":completed", 24, TimeUnit.HOURS);

            Long completedCount = redisTemplate.opsForSet().size(uploadKey + ":completed");
            if (completedCount != null && completedCount.intValue() == totalChunks) {
                log.info("所有分片上传完成，准备合并: userId={}, fileMd5={}", userId, fileMd5);
                return new UploadResultDto(fileMd5, "merge_ready");
            }

            return new UploadResultDto(fileMd5, "uploading");

        } catch (IOException e) {
            log.error("分片上传失败: userId={}, fileMd5={}, chunkIndex={}", userId, fileMd5, chunkIndex, e);
            throw e;
        } finally {
            redisTemplate.opsForSet().remove(uploadKey + ":uploading", chunkIndex);
        }
    }

    /**
     * 获取已上传的分片数量.
     *
     * @param userId 用户ID
     * @param fileMd5 文件MD5值
     * @return 已上传分片数
     */
    public Long getCompletedChunkCount(String userId, String fileMd5) {
        String uploadKey = UPLOAD_PROGRESS_KEY + userId + ":" + fileMd5;
        Long count = redisTemplate.opsForSet().size(uploadKey + ":completed");
        return count != null ? count : 0L;
    }

    /**
     * 检查分片是否已上传.
     *
     * @param userId 用户ID
     * @param fileMd5 文件MD5值
     * @param chunkIndex 分片索引
     * @return 是否已上传
     */
    public boolean isChunkUploaded(String userId, String fileMd5, Integer chunkIndex) {
        String uploadKey = UPLOAD_PROGRESS_KEY + userId + ":" + fileMd5;
        Boolean isMember = redisTemplate.opsForSet().isMember(uploadKey + ":completed", (Object) chunkIndex);
        return Boolean.TRUE.equals(isMember);
    }

    /**
     * 清理上传进度记录.
     *
     * @param userId 用户ID
     * @param fileMd5 文件MD5值
     */
    public void clearUploadProgress(String userId, String fileMd5) {
        String uploadKey = UPLOAD_PROGRESS_KEY + userId + ":" + fileMd5;
        redisTemplate.delete(uploadKey + ":uploading");
        redisTemplate.delete(uploadKey + ":completed");
        log.info("清理上传进度: userId={}, fileMd5={}", userId, fileMd5);
    }

    /**
     * 合并分片文件.
     *
     * <p>使用虚拟线程异步合并，合并完成后清理分片文件
     *
     * @param userId 用户ID
     * @param fileMd5 文件MD5值
     * @param totalChunks 总分片数
     * @return 合并结果
     */
    public UploadResultDto mergeChunks(String userId, String fileMd5, Integer totalChunks) {
        log.info("开始合并分片: userId={}, fileMd5={}, totalChunks={}", userId, fileMd5, totalChunks);

        CompletableFuture.runAsync(() -> {
            try {
                java.io.ByteArrayOutputStream mergedStream = new java.io.ByteArrayOutputStream();

                for (int i = 0; i < totalChunks; i++) {
                    String chunkPath = String.format("chunks/%s/%s/%d", userId, fileMd5, i);

                    try (java.io.InputStream chunkStream = s3Component.getInputStream(chunkPath)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = chunkStream.read(buffer)) != -1) {
                            mergedStream.write(buffer, 0, bytesRead);
                        }
                        log.debug("分片 {} 读取完成", i);
                    } catch (Exception e) {
                        log.error("读取分片失败: chunkPath={}", chunkPath, e);
                        throw new RuntimeException("读取分片失败: " + chunkPath, e);
                    }
                }

                String finalPath = String.format("files/%s/%s", userId, fileMd5);
                byte[] mergedData = mergedStream.toByteArray();
                s3Component.uploadBytes(finalPath, mergedData);
                log.info("合并文件上传成功: {}, size={} bytes", finalPath, mergedData.length);

                for (int i = 0; i < totalChunks; i++) {
                    String chunkPath = String.format("chunks/%s/%s/%d", userId, fileMd5, i);
                    try {
                        s3Component.deleteFile(chunkPath);
                        log.debug("分片 {} 清理完成", i);
                    } catch (Exception e) {
                        log.warn("清理分片失败: chunkPath={}", chunkPath, e);
                    }
                }

                clearUploadProgress(userId, fileMd5);

                log.info("文件合并完成: userId={}, fileMd5={}", userId, fileMd5);

            } catch (Exception e) {
                log.error("文件合并失败: userId={}, fileMd5={}", userId, fileMd5, e);
                clearUploadProgress(userId, fileMd5);
            }
        }, Executors.newVirtualThreadPerTaskExecutor());

        return new UploadResultDto(fileMd5, "merging");
    }

}
