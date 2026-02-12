package com.easypan.unit.service;

import com.easypan.entity.constants.Constants;
import com.easypan.entity.dto.SessionShareDto;
import com.easypan.entity.po.FileShare;
import com.easypan.entity.query.FileShareQuery;
import com.easypan.entity.vo.PaginationResultVO;
import com.easypan.exception.BusinessException;
import com.easypan.mappers.FileShareMapper;
import com.easypan.service.impl.FileShareServiceImpl;
import com.easypan.utils.DateUtil;
import com.easypan.utils.StringTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FileShareService 单元测试
 * 测试分享创建、验证、访问控制、过期等功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FileShareService 单元测试")
class FileShareServiceTest {

    @Mock
    private FileShareMapper fileShareMapper;

    @InjectMocks
    private FileShareServiceImpl fileShareService;

    private FileShare testShare;
    private String testShareId;
    private String testUserId;
    private String testFileId;

    @BeforeEach
    void setUp() {
        testShareId = "test_share_12345";
        testUserId = "user_123";
        testFileId = "file_456";

        testShare = new FileShare();
        testShare.setShareId(testShareId);
        testShare.setUserId(testUserId);
        testShare.setFileId(testFileId);
        testShare.setCode("12345");
        testShare.setValidType(0); // 1天有效
        testShare.setShareTime(new Date());
        testShare.setExpireTime(DateUtil.getAfterDate(1));
        testShare.setShowCount(0);
    }

    // ==================== 分享创建测试 ====================

    @Test
    @DisplayName("创建分享 - 1天有效期")
    void testSaveShare_OneDayValidity() {
        try (MockedStatic<StringTools> stringToolsMock = mockStatic(StringTools.class);
                MockedStatic<DateUtil> dateUtilMock = mockStatic(DateUtil.class)) {

            // Given
            stringToolsMock.when(() -> StringTools.getRandomString(Constants.LENGTH_5))
                    .thenReturn("12345");
            stringToolsMock.when(() -> StringTools.getRandomString(Constants.LENGTH_20))
                    .thenReturn(testShareId);
            stringToolsMock.when(() -> StringTools.isEmpty(any()))
                    .thenReturn(true);

            Date expectedExpireTime = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
            dateUtilMock.when(() -> DateUtil.getAfterDate(1))
                    .thenReturn(expectedExpireTime);

            FileShare newShare = new FileShare();
            newShare.setUserId(testUserId);
            newShare.setFileId(testFileId);
            newShare.setValidType(0); // 1天

            when(fileShareMapper.insert(any(FileShare.class))).thenReturn(1);

            // When
            fileShareService.saveShare(newShare);

            // Then
            verify(fileShareMapper, times(1)).insert(any(FileShare.class));
            assertEquals(testShareId, newShare.getShareId());
            assertEquals("12345", newShare.getCode());
            assertNotNull(newShare.getShareTime());
            assertNotNull(newShare.getExpireTime());
        }
    }

    @Test
    @DisplayName("创建分享 - 永久有效")
    void testSaveShare_ForeverValidity() {
        try (MockedStatic<StringTools> stringToolsMock = mockStatic(StringTools.class)) {

            // Given
            stringToolsMock.when(() -> StringTools.getRandomString(Constants.LENGTH_5))
                    .thenReturn("12345");
            stringToolsMock.when(() -> StringTools.getRandomString(Constants.LENGTH_20))
                    .thenReturn(testShareId);
            stringToolsMock.when(() -> StringTools.isEmpty(any()))
                    .thenReturn(true);

            FileShare newShare = new FileShare();
            newShare.setUserId(testUserId);
            newShare.setFileId(testFileId);
            newShare.setValidType(3); // 永久有效

            when(fileShareMapper.insert(any(FileShare.class))).thenReturn(1);

            // When
            fileShareService.saveShare(newShare);

            // Then
            verify(fileShareMapper, times(1)).insert(any(FileShare.class));
            assertNull(newShare.getExpireTime()); // 永久有效不设置过期时间
        }
    }

    @Test
    @DisplayName("创建分享 - 无效的有效期类型")
    void testSaveShare_InvalidValidityType() {
        // Given
        FileShare newShare = new FileShare();
        newShare.setUserId(testUserId);
        newShare.setFileId(testFileId);
        newShare.setValidType(999); // 无效类型

        // When & Then
        assertThrows(BusinessException.class, () -> {
            fileShareService.saveShare(newShare);
        });

        verify(fileShareMapper, never()).insert(any(FileShare.class));
    }

    @Test
    @DisplayName("创建分享 - 自定义提取码")
    void testSaveShare_CustomCode() {
        try (MockedStatic<StringTools> stringToolsMock = mockStatic(StringTools.class);
                MockedStatic<DateUtil> dateUtilMock = mockStatic(DateUtil.class)) {

            // Given
            String customCode = "abc123";
            stringToolsMock.when(() -> StringTools.getRandomString(Constants.LENGTH_20))
                    .thenReturn(testShareId);
            stringToolsMock.when(() -> StringTools.isEmpty(customCode))
                    .thenReturn(false);

            dateUtilMock.when(() -> DateUtil.getAfterDate(1))
                    .thenReturn(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000));

            FileShare newShare = new FileShare();
            newShare.setUserId(testUserId);
            newShare.setFileId(testFileId);
            newShare.setValidType(0);
            newShare.setCode(customCode);

            when(fileShareMapper.insert(any(FileShare.class))).thenReturn(1);

            // When
            fileShareService.saveShare(newShare);

            // Then
            assertEquals(customCode, newShare.getCode());
            verify(fileShareMapper, times(1)).insert(any(FileShare.class));
        }
    }

    // ==================== 分享验证测试 ====================

    @Test
    @DisplayName("验证分享码 - 成功")
    void testCheckShareCode_Success() {
        // Given
        when(fileShareMapper.selectByShareId(testShareId)).thenReturn(testShare);
        // Void method doesn't need return value stubbing

        // When
        SessionShareDto result = fileShareService.checkShareCode(testShareId, "12345");

        // Then
        assertNotNull(result);
        assertEquals(testShareId, result.getShareId());
        assertEquals(testUserId, result.getShareUserId());
        assertEquals(testFileId, result.getFileId());
        verify(fileShareMapper, times(1)).updateShareShowCount(testShareId);
    }

    @Test
    @DisplayName("验证分享码 - 分享不存在")
    void testCheckShareCode_ShareNotFound() {
        // Given
        when(fileShareMapper.selectByShareId(testShareId)).thenReturn(null);

        // When & Then
        assertThrows(BusinessException.class, () -> {
            fileShareService.checkShareCode(testShareId, "12345");
        });

        verify(fileShareMapper, never()).updateShareShowCount(anyString());
    }

    @Test
    @DisplayName("验证分享码 - 分享已过期")
    void testCheckShareCode_Expired() {
        // Given
        testShare.setExpireTime(new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000)); // 昨天过期
        when(fileShareMapper.selectByShareId(testShareId)).thenReturn(testShare);

        // When & Then
        assertThrows(BusinessException.class, () -> {
            fileShareService.checkShareCode(testShareId, "12345");
        });

        verify(fileShareMapper, never()).updateShareShowCount(anyString());
    }

    @Test
    @DisplayName("验证分享码 - 提取码错误")
    void testCheckShareCode_WrongCode() {
        // Given
        when(fileShareMapper.selectByShareId(testShareId)).thenReturn(testShare);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            fileShareService.checkShareCode(testShareId, "wrong_code");
        });

        assertEquals("提取码错误", exception.getMessage());
        verify(fileShareMapper, never()).updateShareShowCount(anyString());
    }

    // ==================== 批量删除测试 ====================

    @Test
    @DisplayName("批量删除分享 - 成功")
    void testDeleteFileShareBatch_Success() {
        // Given
        String[] shareIds = { "share1", "share2", "share3" };
        when(fileShareMapper.deleteFileShareBatch(shareIds, testUserId)).thenReturn(3);

        // When
        fileShareService.deleteFileShareBatch(shareIds, testUserId);

        // Then
        verify(fileShareMapper, times(1)).deleteFileShareBatch(shareIds, testUserId);
    }

    @Test
    @DisplayName("批量删除分享 - 部分失败")
    void testDeleteFileShareBatch_PartialFailure() {
        // Given
        String[] shareIds = { "share1", "share2", "share3" };
        when(fileShareMapper.deleteFileShareBatch(shareIds, testUserId)).thenReturn(2); // 只删除了2个

        // When & Then
        assertThrows(BusinessException.class, () -> {
            fileShareService.deleteFileShareBatch(shareIds, testUserId);
        });

        verify(fileShareMapper, times(1)).deleteFileShareBatch(shareIds, testUserId);
    }

    // ==================== 分页查询测试 ====================

    @Test
    @DisplayName("分页查询分享列表")
    void testFindListByPage() {
        // Given
        FileShareQuery query = new FileShareQuery();
        query.setUserId(testUserId);
        query.setPageNo(1);
        query.setPageSize(15);

        List<FileShare> shareList = Arrays.asList(testShare);
        when(fileShareMapper.selectCount(any(FileShareQuery.class))).thenReturn(1);
        when(fileShareMapper.selectList(any(FileShareQuery.class))).thenReturn(shareList);

        // When
        PaginationResultVO<FileShare> result = fileShareService.findListByPage(query);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getList().size());
        assertEquals(testShareId, result.getList().get(0).getShareId());
    }

    // ==================== 基础CRUD测试 ====================

    @Test
    @DisplayName("根据ShareId查询分享")
    void testGetFileShareByShareId() {
        // Given
        when(fileShareMapper.selectByShareId(testShareId)).thenReturn(testShare);

        // When
        FileShare result = fileShareService.getFileShareByShareId(testShareId);

        // Then
        assertNotNull(result);
        assertEquals(testShareId, result.getShareId());
        assertEquals(testUserId, result.getUserId());
    }

    @Test
    @DisplayName("根据ShareId删除分享")
    void testDeleteFileShareByShareId() {
        // Given
        when(fileShareMapper.deleteByShareId(testShareId)).thenReturn(1);

        // When
        Integer result = fileShareService.deleteFileShareByShareId(testShareId);

        // Then
        assertEquals(1, result);
        verify(fileShareMapper, times(1)).deleteByShareId(testShareId);
    }

    @Test
    @DisplayName("批量新增分享 - 空列表")
    void testAddBatch_EmptyList() {
        // When
        Integer result = fileShareService.addBatch(null);

        // Then
        assertEquals(Integer.valueOf(0), result);
        verify(fileShareMapper, never()).insertBatch(anyList());
    }

    @Test
    @DisplayName("批量新增分享 - 成功")
    void testAddBatch_Success() {
        // Given
        List<FileShare> shareList = Arrays.asList(testShare);
        when(fileShareMapper.insertBatch(shareList)).thenReturn(1);

        // When
        Integer result = fileShareService.addBatch(shareList);

        // Then
        assertEquals(1, result);
        verify(fileShareMapper, times(1)).insertBatch(shareList);
    }
}
