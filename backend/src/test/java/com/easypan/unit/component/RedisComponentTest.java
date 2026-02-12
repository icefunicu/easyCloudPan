package com.easypan.unit.component;

import com.easypan.component.RedisComponent;
import com.easypan.component.RedisUtils;
import com.easypan.entity.constants.Constants;
import com.easypan.entity.dto.DownloadFileDto;
import com.easypan.entity.dto.SysSettingsDto;
import com.easypan.entity.dto.UserSpaceDto;
import com.easypan.entity.po.UserInfo;
import com.easypan.mappers.FileInfoMapper;
import com.easypan.mappers.UserInfoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RedisComponent 单元测试
 * 测试缓存操作、会话管理、分布式锁等功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RedisComponent 单元测试")
class RedisComponentTest {

    @Mock
    private RedisUtils<Object> redisUtils;

    @Mock
    private UserInfoMapper userInfoMapper;

    @Mock
    private FileInfoMapper fileInfoMapper;

    @InjectMocks
    private RedisComponent redisComponent;

    private String testUserId;
    private String testFileId;
    private SysSettingsDto testSysSettings;
    private UserSpaceDto testUserSpace;
    private UserInfo testUserInfo;

    @BeforeEach
    void setUp() {
        testUserId = "user_123";
        testFileId = "file_456";

        testSysSettings = new SysSettingsDto();
        testSysSettings.setUserInitUseSpace(5); // 5MB

        testUserSpace = new UserSpaceDto();
        testUserSpace.setUseSpace(1024L * 1024L); // 1MB
        testUserSpace.setTotalSpace(5L * 1024L * 1024L); // 5MB

        testUserInfo = new UserInfo();
        testUserInfo.setUserId(testUserId);
        testUserInfo.setTotalSpace(10L * 1024L * 1024L); // 10MB
    }

    // ==================== 系统设置缓存测试 ====================

    @Test
    @DisplayName("获取系统设置 - 缓存命中")
    void testGetSysSettingsDto_CacheHit() {
        // Given
        when(redisUtils.get(Constants.REDIS_KEY_SYS_SETTING)).thenReturn(testSysSettings);

        // When
        SysSettingsDto result = redisComponent.getSysSettingsDto();

        // Then
        assertNotNull(result);
        assertEquals(testSysSettings.getUserInitUseSpace(), result.getUserInitUseSpace());
        verify(redisUtils, times(1)).get(Constants.REDIS_KEY_SYS_SETTING);
        verify(redisUtils, never()).set(anyString(), any());
    }

    @Test
    @DisplayName("获取系统设置 - 缓存未命中，创建默认值")
    void testGetSysSettingsDto_CacheMiss() {
        // Given
        when(redisUtils.get(Constants.REDIS_KEY_SYS_SETTING)).thenReturn(null);

        // When
        SysSettingsDto result = redisComponent.getSysSettingsDto();

        // Then
        assertNotNull(result);
        verify(redisUtils, times(1)).get(Constants.REDIS_KEY_SYS_SETTING);
        verify(redisUtils, times(1)).set(eq(Constants.REDIS_KEY_SYS_SETTING), any(SysSettingsDto.class));
    }

    @Test
    @DisplayName("保存系统设置")
    void testSaveSysSettingsDto() {
        // When
        redisComponent.saveSysSettingsDto(testSysSettings);

        // Then
        verify(redisUtils, times(1)).set(Constants.REDIS_KEY_SYS_SETTING, testSysSettings);
    }

    // ==================== 下载码缓存测试 ====================

    @Test
    @DisplayName("保存下载码")
    void testSaveDownloadCode() {
        // Given
        String code = "download_123";
        DownloadFileDto downloadDto = new DownloadFileDto();
        downloadDto.setFileName("test.txt");
        downloadDto.setFilePath("/files/test.txt");

        // When
        redisComponent.saveDownloadCode(code, downloadDto);

        // Then
        verify(redisUtils, times(1)).setex(
                eq(Constants.REDIS_KEY_DOWNLOAD + code),
                eq(downloadDto),
                eq((long) Constants.REDIS_KEY_EXPIRES_FIVE_MIN)
        );
    }

    @Test
    @DisplayName("获取下载码")
    void testGetDownloadCode() {
        // Given
        String code = "download_123";
        DownloadFileDto downloadDto = new DownloadFileDto();
        downloadDto.setFileName("test.txt");
        when(redisUtils.get(Constants.REDIS_KEY_DOWNLOAD + code)).thenReturn(downloadDto);

        // When
        DownloadFileDto result = redisComponent.getDownloadCode(code);

        // Then
        assertNotNull(result);
        assertEquals("test.txt", result.getFileName());
        verify(redisUtils, times(1)).get(Constants.REDIS_KEY_DOWNLOAD + code);
    }

    @Test
    @DisplayName("获取下载码 - 不存在")
    void testGetDownloadCode_NotFound() {
        // Given
        String code = "invalid_code";
        when(redisUtils.get(Constants.REDIS_KEY_DOWNLOAD + code)).thenReturn(null);

        // When
        DownloadFileDto result = redisComponent.getDownloadCode(code);

        // Then
        assertNull(result);
    }

    // ==================== 用户空间缓存测试 ====================

    @Test
    @DisplayName("获取用户空间使用情况 - 缓存命中")
    void testGetUserSpaceUse_CacheHit() {
        // Given
        when(redisUtils.get(Constants.REDIS_KEY_USER_SPACE_USE + testUserId)).thenReturn(testUserSpace);

        // When
        UserSpaceDto result = redisComponent.getUserSpaceUse(testUserId);

        // Then
        assertNotNull(result);
        assertEquals(testUserSpace.getUseSpace(), result.getUseSpace());
        assertEquals(testUserSpace.getTotalSpace(), result.getTotalSpace());
        verify(fileInfoMapper, never()).selectUseSpace(anyString());
    }

    @Test
    @DisplayName("获取用户空间使用情况 - 缓存未命中")
    void testGetUserSpaceUse_CacheMiss() {
        // Given
        Long usedSpace = 2L * 1024L * 1024L; // 2MB
        when(redisUtils.get(Constants.REDIS_KEY_USER_SPACE_USE + testUserId)).thenReturn(null);
        when(fileInfoMapper.selectUseSpace(testUserId)).thenReturn(usedSpace);
        when(redisUtils.get(Constants.REDIS_KEY_SYS_SETTING)).thenReturn(testSysSettings);

        // When
        UserSpaceDto result = redisComponent.getUserSpaceUse(testUserId);

        // Then
        assertNotNull(result);
        assertEquals(usedSpace, result.getUseSpace());
        assertEquals(5L * Constants.MB, result.getTotalSpace());
        verify(fileInfoMapper, times(1)).selectUseSpace(testUserId);
        verify(redisUtils, times(1)).setex(
                eq(Constants.REDIS_KEY_USER_SPACE_USE + testUserId),
                any(UserSpaceDto.class),
                eq((long) Constants.REDIS_KEY_EXPIRES_DAY)
        );
    }

    @Test
    @DisplayName("保存用户空间使用情况")
    void testSaveUserSpaceUse() {
        // When
        redisComponent.saveUserSpaceUse(testUserId, testUserSpace);

        // Then
        verify(redisUtils, times(1)).setex(
                eq(Constants.REDIS_KEY_USER_SPACE_USE + testUserId),
                eq(testUserSpace),
                eq((long) Constants.REDIS_KEY_EXPIRES_DAY)
        );
    }

    @Test
    @DisplayName("重置用户空间使用情况")
    void testResetUserSpaceUse() {
        // Given
        Long usedSpace = 3L * 1024L * 1024L; // 3MB
        when(fileInfoMapper.selectUseSpace(testUserId)).thenReturn(usedSpace);
        when(userInfoMapper.selectByUserId(testUserId)).thenReturn(testUserInfo);

        // When
        UserSpaceDto result = redisComponent.resetUserSpaceUse(testUserId);

        // Then
        assertNotNull(result);
        assertEquals(usedSpace, result.getUseSpace());
        assertEquals(testUserInfo.getTotalSpace(), result.getTotalSpace());
        verify(fileInfoMapper, times(1)).selectUseSpace(testUserId);
        verify(userInfoMapper, times(1)).selectByUserId(testUserId);
        verify(redisUtils, times(1)).setex(
                eq(Constants.REDIS_KEY_USER_SPACE_USE + testUserId),
                any(UserSpaceDto.class),
                eq((long) Constants.REDIS_KEY_EXPIRES_DAY)
        );
    }

    // ==================== 文件临时大小缓存测试 ====================

    @Test
    @DisplayName("保存文件临时大小 - 首次保存")
    void testSaveFileTempSize_FirstTime() {
        // Given
        Long fileSize = 1024L * 1024L; // 1MB
        String key = Constants.REDIS_KEY_USER_FILE_TEMP_SIZE + testUserId + testFileId;
        when(redisUtils.get(key)).thenReturn(null);

        // When
        redisComponent.saveFileTempSize(testUserId, testFileId, fileSize);

        // Then
        verify(redisUtils, times(1)).get(key);
        verify(redisUtils, times(1)).setex(
                eq(key),
                eq(fileSize),
                eq((long) Constants.REDIS_KEY_EXPIRES_ONE_HOUR)
        );
    }

    @Test
    @DisplayName("保存文件临时大小 - 累加")
    void testSaveFileTempSize_Accumulate() {
        // Given
        Long existingSize = 1024L * 1024L; // 1MB
        Long additionalSize = 512L * 1024L; // 512KB
        String key = Constants.REDIS_KEY_USER_FILE_TEMP_SIZE + testUserId + testFileId;
        when(redisUtils.get(key)).thenReturn(existingSize);

        // When
        redisComponent.saveFileTempSize(testUserId, testFileId, additionalSize);

        // Then
        verify(redisUtils, times(1)).get(key);
        verify(redisUtils, times(1)).setex(
                eq(key),
                eq(existingSize + additionalSize),
                eq((long) Constants.REDIS_KEY_EXPIRES_ONE_HOUR)
        );
    }

    @Test
    @DisplayName("获取文件临时大小 - 存在")
    void testGetFileTempSize_Exists() {
        // Given
        Long fileSize = 2L * 1024L * 1024L; // 2MB
        String key = Constants.REDIS_KEY_USER_FILE_TEMP_SIZE + testUserId + testFileId;
        when(redisUtils.get(key)).thenReturn(fileSize);

        // When
        Long result = redisComponent.getFileTempSize(testUserId, testFileId);

        // Then
        assertEquals(fileSize, result);
    }

    @Test
    @DisplayName("获取文件临时大小 - 不存在")
    void testGetFileTempSize_NotExists() {
        // Given
        String key = Constants.REDIS_KEY_USER_FILE_TEMP_SIZE + testUserId + testFileId;
        when(redisUtils.get(key)).thenReturn(null);

        // When
        Long result = redisComponent.getFileTempSize(testUserId, testFileId);

        // Then
        assertEquals(0L, result);
    }

    @Test
    @DisplayName("获取文件临时大小 - Integer类型转换")
    void testGetFileTempSize_IntegerType() {
        // Given
        Integer fileSize = 1024 * 1024; // 1MB as Integer
        String key = Constants.REDIS_KEY_USER_FILE_TEMP_SIZE + testUserId + testFileId;
        when(redisUtils.get(key)).thenReturn(fileSize);

        // When
        Long result = redisComponent.getFileTempSize(testUserId, testFileId);

        // Then
        assertEquals(fileSize.longValue(), result);
    }

    // ==================== 边界条件测试 ====================

    @Test
    @DisplayName("获取用户空间 - 数据库返回null")
    void testGetUserSpaceUse_DatabaseReturnsNull() {
        // Given
        when(redisUtils.get(Constants.REDIS_KEY_USER_SPACE_USE + testUserId)).thenReturn(null);
        when(fileInfoMapper.selectUseSpace(testUserId)).thenReturn(null);
        when(redisUtils.get(Constants.REDIS_KEY_SYS_SETTING)).thenReturn(testSysSettings);

        // When
        UserSpaceDto result = redisComponent.getUserSpaceUse(testUserId);

        // Then
        assertNotNull(result);
        assertNull(result.getUseSpace()); // 应该保持null
        assertEquals(5L * Constants.MB, result.getTotalSpace());
    }

    @Test
    @DisplayName("保存文件临时大小 - 零大小")
    void testSaveFileTempSize_ZeroSize() {
        // Given
        String key = Constants.REDIS_KEY_USER_FILE_TEMP_SIZE + testUserId + testFileId;
        when(redisUtils.get(key)).thenReturn(null);

        // When
        redisComponent.saveFileTempSize(testUserId, testFileId, 0L);

        // Then
        verify(redisUtils, times(1)).setex(
                eq(key),
                eq(0L),
                eq((long) Constants.REDIS_KEY_EXPIRES_ONE_HOUR)
        );
    }

    @Test
    @DisplayName("保存文件临时大小 - 大文件")
    void testSaveFileTempSize_LargeFile() {
        // Given
        Long largeSize = 5L * 1024L * 1024L * 1024L; // 5GB
        String key = Constants.REDIS_KEY_USER_FILE_TEMP_SIZE + testUserId + testFileId;
        when(redisUtils.get(key)).thenReturn(null);

        // When
        redisComponent.saveFileTempSize(testUserId, testFileId, largeSize);

        // Then
        verify(redisUtils, times(1)).setex(
                eq(key),
                eq(largeSize),
                eq((long) Constants.REDIS_KEY_EXPIRES_ONE_HOUR)
        );
    }
}
