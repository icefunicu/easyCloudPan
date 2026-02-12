package com.easypan.unit.service;

import com.easypan.component.S3Component;
import com.easypan.entity.dto.UploadResultDto;
import com.easypan.exception.BusinessException;
import com.easypan.service.ChunkUploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ChunkUploadService 单元测试
 * 验证需求：2.3.1 - 分片上传并发控制
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("分片上传服务测试")
class ChunkUploadServiceTest {

    @Mock
    private S3Component s3Component;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private SetOperations<String, Object> setOperations;

    @Mock
    private MultipartFile chunkFile;

    @InjectMocks
    private ChunkUploadService chunkUploadService;

    @BeforeEach
    void setUp() {
        // Setup is done by @Mock and @InjectMocks annotations
        // Only configure common behavior here if needed by all tests
    }

    @Test
    @DisplayName("正常上传分片应该成功")
    void testUploadChunk_Success() throws IOException {
        // Given
        String userId = "user123";
        String fileMd5 = "abc123def456";
        Integer chunkIndex = 0;
        Integer totalChunks = 10;
        byte[] chunkData = "test chunk data".getBytes();

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(chunkFile.getBytes()).thenReturn(chunkData);
        when(setOperations.size(anyString())).thenReturn(0L); // 当前无并发
        when(setOperations.add(anyString(), any())).thenReturn(1L);

        // When
        UploadResultDto result = chunkUploadService.uploadChunk(userId, fileMd5, chunkIndex, totalChunks, chunkFile);

        // Then
        assertNotNull(result);
        assertEquals("uploading", result.getStatus());
        assertEquals(fileMd5, result.getFileId());

        // 验证上传到 S3
        verify(s3Component, times(1)).uploadBytes(
                eq(String.format("chunks/%s/%s/%d", userId, fileMd5, chunkIndex)),
                eq(chunkData)
        );

        // 验证 Redis 操作
        verify(setOperations, times(1)).add(contains(":uploading"), eq(chunkIndex));
        verify(setOperations, times(1)).add(contains(":completed"), eq(chunkIndex));
        verify(setOperations, times(1)).remove(contains(":uploading"), eq(chunkIndex));
    }

    @Test
    @DisplayName("并发数超限应该抛出异常")
    void testUploadChunk_ConcurrencyLimitExceeded() throws IOException {
        // Given
        String userId = "user123";
        String fileMd5 = "abc123def456";
        Integer chunkIndex = 0;
        Integer totalChunks = 10;

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.size(anyString())).thenReturn(5L); // 已达到最大并发数

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            chunkUploadService.uploadChunk(userId, fileMd5, chunkIndex, totalChunks, chunkFile);
        });

        assertTrue(exception.getMessage().contains("并发上传数超限"));

        // 验证没有上传到 S3
        verify(s3Component, never()).uploadBytes(anyString(), any());
    }

    @Test
    @DisplayName("所有分片上传完成应该返回 merge_ready 状态")
    void testUploadChunk_AllChunksCompleted() throws IOException {
        // Given
        String userId = "user123";
        String fileMd5 = "abc123def456";
        Integer chunkIndex = 9; // 最后一个分片
        Integer totalChunks = 10;
        byte[] chunkData = "test chunk data".getBytes();

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(chunkFile.getBytes()).thenReturn(chunkData);
        when(setOperations.size(contains(":uploading"))).thenReturn(0L);
        when(setOperations.size(contains(":completed"))).thenReturn(10L); // 所有分片已完成
        when(setOperations.add(anyString(), any())).thenReturn(1L);

        // When
        UploadResultDto result = chunkUploadService.uploadChunk(userId, fileMd5, chunkIndex, totalChunks, chunkFile);

        // Then
        assertNotNull(result);
        assertEquals("merge_ready", result.getStatus());
        assertEquals(fileMd5, result.getFileId());
    }

    @Test
    @DisplayName("检查分片是否已上传")
    void testIsChunkUploaded() {
        // Given
        String userId = "user123";
        String fileMd5 = "abc123def456";
        Integer chunkIndex = 5;

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.isMember(anyString(), any(Object.class))).thenReturn(Boolean.TRUE);

        // When
        boolean result = chunkUploadService.isChunkUploaded(userId, fileMd5, chunkIndex);

        // Then
        assertTrue(result);
        verify(setOperations, times(1)).isMember(contains(":completed"), any(Object.class));
    }

    @Test
    @DisplayName("获取已上传分片数量")
    void testGetCompletedChunkCount() {
        // Given
        String userId = "user123";
        String fileMd5 = "abc123def456";

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.size(anyString())).thenReturn(7L);

        // When
        Long count = chunkUploadService.getCompletedChunkCount(userId, fileMd5);

        // Then
        assertEquals(7L, count);
    }

    @Test
    @DisplayName("清理上传进度")
    void testClearUploadProgress() {
        // Given
        String userId = "user123";
        String fileMd5 = "abc123def456";

        when(redisTemplate.delete(anyString())).thenReturn(true);

        // When
        chunkUploadService.clearUploadProgress(userId, fileMd5);

        // Then
        verify(redisTemplate, times(2)).delete(anyString());
    }

    @Test
    @DisplayName("上传失败应该移除上传中标记")
    void testUploadChunk_FailureRemovesUploadingFlag() throws IOException {
        // Given
        String userId = "user123";
        String fileMd5 = "abc123def456";
        Integer chunkIndex = 0;
        Integer totalChunks = 10;

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(chunkFile.getBytes()).thenThrow(new IOException("读取文件失败"));
        when(setOperations.size(anyString())).thenReturn(0L);
        when(setOperations.add(anyString(), any())).thenReturn(1L);

        // When & Then
        assertThrows(IOException.class, () -> {
            chunkUploadService.uploadChunk(userId, fileMd5, chunkIndex, totalChunks, chunkFile);
        });

        // 验证即使失败也会移除上传中标记
        verify(setOperations, times(1)).remove(contains(":uploading"), eq(chunkIndex));
    }

    @Test
    @DisplayName("合并分片应该返回 merging 状态")
    void testMergeChunks_ReturnsCorrectStatus() {
        // Given
        String userId = "user123";
        String fileMd5 = "abc123def456";
        Integer totalChunks = 3;

        // When
        UploadResultDto result = chunkUploadService.mergeChunks(userId, fileMd5, totalChunks);

        // Then
        assertNotNull(result);
        assertEquals("merging", result.getStatus());
        assertEquals(fileMd5, result.getFileId());
    }
}
