package com.easypan.unit.service;

import com.easypan.entity.dto.UploadResultDto;
import com.easypan.entity.enums.FileDelFlagEnums;
import com.easypan.entity.enums.FileStatusEnums;
import com.easypan.entity.po.FileInfo;
import com.easypan.entity.query.FileInfoQuery;
import com.easypan.mappers.FileInfoMapper;
import com.easypan.service.QuickUploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * QuickUploadService 单元测试
 * 
 * 测试秒传功能的核心逻辑：
 * 1. MD5 查重
 * 2. Redis 缓存
 * 3. 文件引用创建
 * 
 * 需求：2.3.3
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("QuickUploadService 单元测试")
class QuickUploadServiceTest {

    @Mock
    private FileInfoMapper fileInfoMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private QuickUploadService quickUploadService;

    private FileInfo existingFile;
    private static final String TEST_USER_ID = "user123";
    private static final String TEST_FILE_MD5 = "d41d8cd98f00b204e9800998ecf8427e";
    private static final String TEST_FILE_NAME = "test.txt";
    private static final String TEST_FILE_PID = "0";

    @BeforeEach
    void setUp() {
        // 模拟 Redis ValueOperations (使用 lenient 避免不必要的 stubbing 警告)
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // 创建测试用的已存在文件
        existingFile = new FileInfo();
        existingFile.setFileId("file123");
        existingFile.setUserId("user999");
        existingFile.setFileMd5(TEST_FILE_MD5);
        existingFile.setFileName("original.txt");
        existingFile.setFilePath("/path/to/file");
        existingFile.setFileSize(1024L);
        existingFile.setFileCover("cover.jpg");
        existingFile.setFileCategory(4);
        existingFile.setFileType(7);
        existingFile.setFolderType(0);
        existingFile.setStatus(FileStatusEnums.USING.getStatus());
        existingFile.setDelFlag(FileDelFlagEnums.USING.getFlag());
        existingFile.setCreateTime(new Date());
    }

    @Test
    @DisplayName("秒传成功 - Redis 缓存命中")
    void testQuickUpload_RedisCacheHit() {
        // Given: Redis 缓存中存在 MD5 映射
        when(valueOperations.get("file:md5:" + TEST_FILE_MD5)).thenReturn("file123");
        when(fileInfoMapper.selectList(any(FileInfoQuery.class)))
            .thenReturn(List.of(existingFile));
        when(fileInfoMapper.insert(any(FileInfo.class))).thenReturn(1);

        // When: 检查秒传
        UploadResultDto result = quickUploadService.checkQuickUpload(
            TEST_USER_ID, TEST_FILE_MD5, TEST_FILE_NAME, TEST_FILE_PID);

        // Then: 应该返回秒传成功结果
        assertNotNull(result);
        assertEquals("upload_seconds", result.getStatus());
        assertNotNull(result.getFileId());

        // 验证查询了 Redis
        verify(valueOperations).get("file:md5:" + TEST_FILE_MD5);
        
        // 验证插入了新文件记录
        ArgumentCaptor<FileInfo> fileCaptor = ArgumentCaptor.forClass(FileInfo.class);
        verify(fileInfoMapper).insert(fileCaptor.capture());
        
        FileInfo insertedFile = fileCaptor.getValue();
        assertEquals(TEST_USER_ID, insertedFile.getUserId());
        assertEquals(TEST_FILE_NAME, insertedFile.getFileName());
        assertEquals(TEST_FILE_MD5, insertedFile.getFileMd5());
        assertEquals(existingFile.getFilePath(), insertedFile.getFilePath());
        assertEquals(existingFile.getFileSize(), insertedFile.getFileSize());
    }

    @Test
    @DisplayName("秒传成功 - Redis 缓存未命中，数据库查询命中")
    void testQuickUpload_DatabaseHit() {
        // Given: Redis 缓存未命中，但数据库中存在文件
        when(valueOperations.get("file:md5:" + TEST_FILE_MD5)).thenReturn(null);
        when(fileInfoMapper.selectList(any(FileInfoQuery.class)))
            .thenReturn(List.of(existingFile));
        when(fileInfoMapper.insert(any(FileInfo.class))).thenReturn(1);

        // When: 检查秒传
        UploadResultDto result = quickUploadService.checkQuickUpload(
            TEST_USER_ID, TEST_FILE_MD5, TEST_FILE_NAME, TEST_FILE_PID);

        // Then: 应该返回秒传成功结果
        assertNotNull(result);
        assertEquals("upload_seconds", result.getStatus());

        // 验证缓存了 MD5 映射
        verify(valueOperations).set(
            eq("file:md5:" + TEST_FILE_MD5),
            eq(existingFile.getFileId()),
            eq(7L),
            eq(TimeUnit.DAYS)
        );

        // 验证插入了新文件记录
        verify(fileInfoMapper).insert(any(FileInfo.class));
    }

    @Test
    @DisplayName("秒传失败 - 文件不存在")
    void testQuickUpload_FileNotExists() {
        // Given: Redis 和数据库都没有该文件
        when(valueOperations.get("file:md5:" + TEST_FILE_MD5)).thenReturn(null);
        when(fileInfoMapper.selectList(any(FileInfoQuery.class)))
            .thenReturn(Collections.emptyList());

        // When: 检查秒传
        UploadResultDto result = quickUploadService.checkQuickUpload(
            TEST_USER_ID, TEST_FILE_MD5, TEST_FILE_NAME, TEST_FILE_PID);

        // Then: 应该返回 null，表示无法秒传
        assertNull(result);

        // 验证没有插入新文件记录
        verify(fileInfoMapper, never()).insert(any(FileInfo.class));
        
        // 验证没有缓存 MD5 映射
        verify(valueOperations, never()).set(anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("缓存 MD5 映射")
    void testCacheMd5Mapping() {
        // Given
        String fileMd5 = TEST_FILE_MD5;
        String fileId = "file123";

        // When: 缓存 MD5 映射
        quickUploadService.cacheMd5Mapping(fileMd5, fileId);

        // Then: 应该调用 Redis 设置缓存
        verify(valueOperations).set(
            eq("file:md5:" + fileMd5),
            eq(fileId),
            eq(7L),
            eq(TimeUnit.DAYS)
        );
    }

    @Test
    @DisplayName("缓存 MD5 映射 - 参数为空时跳过")
    void testCacheMd5Mapping_SkipWhenEmpty() {
        // When: MD5 为空
        quickUploadService.cacheMd5Mapping(null, "file123");

        // Then: 不应该调用 Redis
        verify(valueOperations, never()).set(anyString(), any(), anyLong(), any(TimeUnit.class));

        // When: FileId 为空
        quickUploadService.cacheMd5Mapping(TEST_FILE_MD5, null);

        // Then: 不应该调用 Redis
        verify(valueOperations, never()).set(anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("清除 MD5 缓存")
    void testClearMd5Cache() {
        // Given
        String fileMd5 = TEST_FILE_MD5;

        // When: 清除 MD5 缓存
        quickUploadService.clearMd5Cache(fileMd5);

        // Then: 应该调用 Redis 删除缓存
        verify(redisTemplate).delete("file:md5:" + fileMd5);
    }

    @Test
    @DisplayName("清除 MD5 缓存 - MD5 为空时跳过")
    void testClearMd5Cache_SkipWhenEmpty() {
        // When: MD5 为空
        quickUploadService.clearMd5Cache(null);

        // Then: 不应该调用 Redis
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("文件引用创建 - 验证所有字段正确复制")
    void testCreateFileReference_AllFieldsCopied() {
        // Given: 数据库中存在文件
        when(valueOperations.get("file:md5:" + TEST_FILE_MD5)).thenReturn(null);
        when(fileInfoMapper.selectList(any(FileInfoQuery.class)))
            .thenReturn(List.of(existingFile));
        when(fileInfoMapper.insert(any(FileInfo.class))).thenReturn(1);

        // When: 检查秒传
        quickUploadService.checkQuickUpload(
            TEST_USER_ID, TEST_FILE_MD5, TEST_FILE_NAME, TEST_FILE_PID);

        // Then: 验证新文件的所有字段
        ArgumentCaptor<FileInfo> fileCaptor = ArgumentCaptor.forClass(FileInfo.class);
        verify(fileInfoMapper).insert(fileCaptor.capture());
        
        FileInfo newFile = fileCaptor.getValue();
        
        // 验证用户相关字段
        assertEquals(TEST_USER_ID, newFile.getUserId());
        assertEquals(TEST_FILE_NAME, newFile.getFileName());
        assertEquals(TEST_FILE_PID, newFile.getFilePid());
        
        // 验证从原文件复制的字段
        assertEquals(existingFile.getFileMd5(), newFile.getFileMd5());
        assertEquals(existingFile.getFilePath(), newFile.getFilePath());
        assertEquals(existingFile.getFileSize(), newFile.getFileSize());
        assertEquals(existingFile.getFileCover(), newFile.getFileCover());
        assertEquals(existingFile.getFileCategory(), newFile.getFileCategory());
        assertEquals(existingFile.getFileType(), newFile.getFileType());
        assertEquals(existingFile.getFolderType(), newFile.getFolderType());
        
        // 验证状态字段
        assertEquals(FileStatusEnums.USING.getStatus(), newFile.getStatus());
        assertEquals(FileDelFlagEnums.USING.getFlag(), newFile.getDelFlag());
        
        // 验证时间字段
        assertNotNull(newFile.getCreateTime());
        assertNotNull(newFile.getLastUpdateTime());
        
        // 验证文件 ID 不为空且不同于原文件
        assertNotNull(newFile.getFileId());
        assertNotEquals(existingFile.getFileId(), newFile.getFileId());
    }
}
