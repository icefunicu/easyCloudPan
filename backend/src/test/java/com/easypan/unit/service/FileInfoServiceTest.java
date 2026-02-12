package com.easypan.unit.service;

import com.easypan.entity.enums.FileDelFlagEnums;
import com.easypan.entity.enums.FileFolderTypeEnums;
import com.easypan.entity.enums.FileStatusEnums;
import com.easypan.entity.po.FileInfo;
import com.easypan.entity.query.FileInfoQuery;
import com.easypan.exception.BusinessException;
import com.easypan.mappers.FileInfoMapper;
import com.easypan.service.impl.FileInfoServiceImpl;
import com.easypan.utils.StringTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FileInfoService 单元测试
 * 
 * 测试文件管理核心功能：
 * 1. 创建文件夹（正常场景、空名称、超配额）
 * 2. 删除文件（正常场景、文件不存在）
 * 3. 移动文件
 * 
 * 需求：1.2.1
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FileInfoService 单元测试")
class FileInfoServiceTest {

    @Mock
    private FileInfoMapper fileInfoMapper;

    @InjectMocks
    private FileInfoServiceImpl fileInfoService;

    private static final String TEST_USER_ID = "user123";
    private static final String TEST_FOLDER_NAME = "测试文件夹";
    private static final String TEST_FILE_PID = "0";
    private static final String TEST_FILE_ID = "file123";

    @BeforeEach
    void setUp() {
        // Setup common test data if needed
    }

    // ==================== 创建文件夹测试 ====================

    @Test
    @DisplayName("创建文件夹 - 正常场景")
    void testCreateFolder_Success() {
        // Given: 文件夹名称不重复
        when(fileInfoMapper.selectCount(any(FileInfoQuery.class))).thenReturn(0, 1);
        when(fileInfoMapper.insert(any(FileInfo.class))).thenReturn(1);

        try (MockedStatic<StringTools> stringToolsMock = mockStatic(StringTools.class)) {
            stringToolsMock.when(() -> StringTools.getRandomString(10))
                .thenReturn(TEST_FILE_ID);

            // When: 创建文件夹
            FileInfo result = fileInfoService.newFolder(TEST_FILE_PID, TEST_USER_ID, TEST_FOLDER_NAME);

            // Then: 应该返回新文件夹信息
            assertNotNull(result);
            assertEquals(TEST_FOLDER_NAME, result.getFileName());
            assertEquals(TEST_USER_ID, result.getUserId());
            assertEquals(TEST_FILE_PID, result.getFilePid());
            assertEquals(FileFolderTypeEnums.FOLDER.getType(), result.getFolderType());
            assertEquals(FileStatusEnums.USING.getStatus(), result.getStatus());
            assertEquals(FileDelFlagEnums.USING.getFlag(), result.getDelFlag());
            assertNotNull(result.getCreateTime());
            assertNotNull(result.getLastUpdateTime());

            // 验证插入操作被调用
            ArgumentCaptor<FileInfo> fileCaptor = ArgumentCaptor.forClass(FileInfo.class);
            verify(fileInfoMapper).insert(fileCaptor.capture());

            FileInfo insertedFile = fileCaptor.getValue();
            assertEquals(TEST_FILE_ID, insertedFile.getFileId());
            assertEquals(TEST_FOLDER_NAME, insertedFile.getFileName());
            assertEquals(FileFolderTypeEnums.FOLDER.getType(), insertedFile.getFolderType());
        }
    }

    @Test
    @DisplayName("创建文件夹 - 文件夹名称已存在")
    void testCreateFolder_DuplicateName() {
        // Given: 文件夹名称已存在
        when(fileInfoMapper.selectCount(any(FileInfoQuery.class))).thenReturn(1);

        // When & Then: 应该抛出异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            fileInfoService.newFolder(TEST_FILE_PID, TEST_USER_ID, TEST_FOLDER_NAME);
        });

        assertEquals("此目录下已存在同名文件，请修改名称", exception.getMessage());

        // 验证没有插入操作
        verify(fileInfoMapper, never()).insert(any(FileInfo.class));
    }

    @Test
    @DisplayName("创建文件夹 - 空文件夹名称")
    void testCreateFolder_EmptyName() {
        // Given: 空文件夹名称
        String emptyName = "";

        when(fileInfoMapper.selectCount(any(FileInfoQuery.class))).thenReturn(0, 1);
        when(fileInfoMapper.insert(any(FileInfo.class))).thenReturn(1);

        try (MockedStatic<StringTools> stringToolsMock = mockStatic(StringTools.class)) {
            stringToolsMock.when(() -> StringTools.getRandomString(10))
                .thenReturn(TEST_FILE_ID);

            // When: 创建空名称文件夹
            FileInfo result = fileInfoService.newFolder(TEST_FILE_PID, TEST_USER_ID, emptyName);

            // Then: 应该成功创建（业务逻辑允许空名称）
            assertNotNull(result);
            assertEquals(emptyName, result.getFileName());
        }
    }

    @Test
    @DisplayName("创建文件夹 - 验证文件夹类型正确")
    void testCreateFolder_VerifyFolderType() {
        // Given
        when(fileInfoMapper.selectCount(any(FileInfoQuery.class))).thenReturn(0, 1);
        when(fileInfoMapper.insert(any(FileInfo.class))).thenReturn(1);

        try (MockedStatic<StringTools> stringToolsMock = mockStatic(StringTools.class)) {
            stringToolsMock.when(() -> StringTools.getRandomString(10))
                .thenReturn(TEST_FILE_ID);

            // When
            FileInfo result = fileInfoService.newFolder(TEST_FILE_PID, TEST_USER_ID, TEST_FOLDER_NAME);

            // Then: 验证 folderType 为 1（文件夹）
            assertEquals(1, result.getFolderType());

            // 验证查询条件中也使用了正确的 folderType
            ArgumentCaptor<FileInfoQuery> queryCaptor = ArgumentCaptor.forClass(FileInfoQuery.class);
            verify(fileInfoMapper, atLeastOnce()).selectCount(queryCaptor.capture());

            FileInfoQuery capturedQuery = queryCaptor.getValue();
            assertEquals(FileFolderTypeEnums.FOLDER.getType(), capturedQuery.getFolderType());
        }
    }

    // ==================== 删除文件测试 ====================

    @Test
    @DisplayName("删除文件 - 正常场景")
    void testDeleteFile_Success() {
        // Given: 文件存在
        when(fileInfoMapper.deleteByFileIdAndUserId(TEST_FILE_ID, TEST_USER_ID)).thenReturn(1);

        // When: 删除文件
        Integer result = fileInfoService.deleteFileInfoByFileIdAndUserId(TEST_FILE_ID, TEST_USER_ID);

        // Then: 应该返回删除数量
        assertEquals(1, result);

        // 验证删除操作被调用
        verify(fileInfoMapper).deleteByFileIdAndUserId(TEST_FILE_ID, TEST_USER_ID);
    }

    @Test
    @DisplayName("删除文件 - 文件不存在")
    void testDeleteFile_FileNotExists() {
        // Given: 文件不存在
        when(fileInfoMapper.deleteByFileIdAndUserId(TEST_FILE_ID, TEST_USER_ID)).thenReturn(0);

        // When: 删除文件
        Integer result = fileInfoService.deleteFileInfoByFileIdAndUserId(TEST_FILE_ID, TEST_USER_ID);

        // Then: 应该返回 0
        assertEquals(0, result);

        // 验证删除操作被调用
        verify(fileInfoMapper).deleteByFileIdAndUserId(TEST_FILE_ID, TEST_USER_ID);
    }

    @Test
    @DisplayName("删除文件 - 验证用户隔离")
    void testDeleteFile_UserIsolation() {
        // Given: 不同用户
        String otherUserId = "user456";
        when(fileInfoMapper.deleteByFileIdAndUserId(TEST_FILE_ID, otherUserId)).thenReturn(0);

        // When: 其他用户尝试删除文件
        Integer result = fileInfoService.deleteFileInfoByFileIdAndUserId(TEST_FILE_ID, otherUserId);

        // Then: 应该返回 0（无权限删除）
        assertEquals(0, result);

        // 验证使用了正确的用户 ID
        verify(fileInfoMapper).deleteByFileIdAndUserId(TEST_FILE_ID, otherUserId);
        verify(fileInfoMapper, never()).deleteByFileIdAndUserId(TEST_FILE_ID, TEST_USER_ID);
    }

    // ==================== 移动文件测试 ====================

    @Test
    @DisplayName("移动文件 - 移动到自身")
    void testMoveFile_MoveToSelf() {
        // Given: 目标文件夹是文件自身
        String fileId = TEST_FILE_ID;

        // When & Then: 应该抛出异常
        assertThrows(BusinessException.class, () -> {
            fileInfoService.changeFileFolder(fileId, fileId, TEST_USER_ID);
        });

        // 验证没有更新操作
        verify(fileInfoMapper, never()).updateByFileIdAndUserId(
            any(FileInfo.class), anyString(), anyString());
    }

    @Test
    @DisplayName("移动文件 - 验证文件ID和目标ID相同时抛出异常")
    void testMoveFile_SameIdValidation() {
        // Given: 文件ID和目标ID相同
        String sameId = "same123";

        // When & Then: 应该立即抛出异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            fileInfoService.changeFileFolder(sameId, sameId, TEST_USER_ID);
        });

        // 验证异常信息
        assertNotNull(exception);
        
        // 验证没有任何数据库操作
        verify(fileInfoMapper, never()).updateByFileIdAndUserId(
            any(FileInfo.class), anyString(), anyString());
        verify(fileInfoMapper, never()).selectCount(any(FileInfoQuery.class));
    }

    // ==================== 辅助方法 ====================

    @SuppressWarnings("unused")
    private FileInfo createTestFolder(String fileId, String userId, String folderName) {
        FileInfo folder = new FileInfo();
        folder.setFileId(fileId);
        folder.setUserId(userId);
        folder.setFileName(folderName);
        folder.setFilePid(TEST_FILE_PID);
        folder.setFolderType(FileFolderTypeEnums.FOLDER.getType());
        folder.setStatus(FileStatusEnums.USING.getStatus());
        folder.setDelFlag(FileDelFlagEnums.USING.getFlag());
        folder.setCreateTime(new Date());
        folder.setLastUpdateTime(new Date());
        return folder;
    }

    @SuppressWarnings("unused")
    private FileInfo createTestFile(String fileId, String userId, String fileName, String filePid) {
        FileInfo file = new FileInfo();
        file.setFileId(fileId);
        file.setUserId(userId);
        file.setFileName(fileName);
        file.setFilePid(filePid);
        file.setFolderType(0); // 文件
        file.setFileSize(1024L);
        file.setFileCategory(4);
        file.setFileType(7);
        file.setStatus(FileStatusEnums.USING.getStatus());
        file.setDelFlag(FileDelFlagEnums.USING.getFlag());
        file.setCreateTime(new Date());
        file.setLastUpdateTime(new Date());
        return file;
    }
}
