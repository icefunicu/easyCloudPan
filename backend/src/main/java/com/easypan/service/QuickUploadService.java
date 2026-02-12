package com.easypan.service;

import com.easypan.entity.dto.UploadResultDto;
import com.easypan.entity.enums.FileDelFlagEnums;
import com.easypan.entity.enums.FileStatusEnums;
import com.easypan.entity.po.FileInfo;
import com.easypan.entity.query.FileInfoQuery;
import com.easypan.entity.query.SimplePage;
import com.easypan.mappers.FileInfoMapper;
import com.easypan.utils.StringTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 秒传服务
 * 
 * 功能说明：
 * 1. 通过文件 MD5 值检查文件是否已存在
 * 2. 如果文件已存在，直接创建文件引用，实现秒传
 * 3. 使用 Redis 缓存 MD5 到文件 ID 的映射，提升查询性能
 * 
 * Redis Key 设计：
 * - file:md5:{md5} - String 类型，存储文件 ID，TTL 7天
 * 
 * 需求：2.3.3
 * 
 * @author EasyCloudPan Team
 * @since 2024-01-15
 */
@Service
@Slf4j
public class QuickUploadService {

    @Autowired
    private FileInfoMapper fileInfoMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String FILE_MD5_KEY = "file:md5:";
    private static final long MD5_CACHE_TTL_DAYS = 7;

    /**
     * 检查文件是否可以秒传
     * 
     * 流程：
     * 1. 查询 Redis 缓存，检查 MD5 是否存在
     * 2. 如果缓存未命中，查询数据库
     * 3. 如果数据库中存在该 MD5 的文件，缓存映射关系
     * 4. 创建文件引用，实现秒传
     * 
     * @param userId 用户ID
     * @param fileMd5 文件MD5值
     * @param fileName 文件名称
     * @param filePid 父文件夹ID
     * @return 秒传结果，如果可以秒传返回新文件信息，否则返回 null
     */
    public UploadResultDto checkQuickUpload(String userId, String fileMd5, String fileName, String filePid) {
        log.debug("检查秒传 - userId: {}, fileMd5: {}, fileName: {}", userId, fileMd5, fileName);

        // 1. 查询 Redis 缓存
        String md5Key = FILE_MD5_KEY + fileMd5;
        String cachedFileId = (String) redisTemplate.opsForValue().get(md5Key);

        FileInfo existingFile = null;

        if (cachedFileId != null) {
            // 缓存命中，查询文件详情
            log.debug("Redis 缓存命中 - fileMd5: {}, cachedFileId: {}", fileMd5, cachedFileId);
            
            FileInfoQuery query = new FileInfoQuery();
            query.setFileId(cachedFileId);
            query.setStatus(FileStatusEnums.USING.getStatus());
            
            List<FileInfo> fileList = fileInfoMapper.selectList(query);
            if (!fileList.isEmpty()) {
                existingFile = fileList.get(0);
            }
        }

        // 2. 如果缓存未命中或文件已被删除，查询数据库
        if (existingFile == null) {
            log.debug("Redis 缓存未命中，查询数据库 - fileMd5: {}", fileMd5);
            
            FileInfoQuery query = new FileInfoQuery();
            query.setFileMd5(fileMd5);
            query.setStatus(FileStatusEnums.USING.getStatus());
            query.setSimplePage(new SimplePage(0, 1));
            
            List<FileInfo> fileList = fileInfoMapper.selectList(query);
            
            if (!fileList.isEmpty()) {
                existingFile = fileList.get(0);
                
                // 3. 缓存 MD5 映射关系
                redisTemplate.opsForValue().set(md5Key, existingFile.getFileId(), 
                    MD5_CACHE_TTL_DAYS, TimeUnit.DAYS);
                
                log.info("缓存 MD5 映射 - fileMd5: {}, fileId: {}", fileMd5, existingFile.getFileId());
            }
        }

        // 4. 如果文件存在，创建文件引用
        if (existingFile != null) {
            FileInfo newFile = createFileReference(userId, existingFile, fileName, filePid);
            
            log.info("秒传成功 - userId: {}, newFileId: {}, sourceFileId: {}, fileName: {}", 
                userId, newFile.getFileId(), existingFile.getFileId(), fileName);
            
            UploadResultDto result = new UploadResultDto();
            result.setFileId(newFile.getFileId());
            result.setStatus("upload_seconds");
            
            return result;
        }

        log.debug("无法秒传，需要正常上传 - userId: {}, fileMd5: {}", userId, fileMd5);
        return null;
    }

    /**
     * 创建文件引用
     * 
     * 复制已存在文件的元数据，创建新的文件记录
     * 注意：不复制实际文件内容，只是创建引用
     * 
     * @param userId 用户ID
     * @param existingFile 已存在的文件
     * @param fileName 新文件名称
     * @param filePid 父文件夹ID
     * @return 新创建的文件信息
     */
    private FileInfo createFileReference(String userId, FileInfo existingFile, String fileName, String filePid) {
        FileInfo newFile = new FileInfo();
        
        // 生成新的文件ID
        newFile.setFileId(StringTools.getRandomString(10));
        newFile.setUserId(userId);
        newFile.setFileMd5(existingFile.getFileMd5());
        newFile.setFilePid(filePid);
        newFile.setFileName(fileName);
        newFile.setFilePath(existingFile.getFilePath());
        newFile.setFileSize(existingFile.getFileSize());
        newFile.setFileCover(existingFile.getFileCover());
        newFile.setFileCategory(existingFile.getFileCategory());
        newFile.setFileType(existingFile.getFileType());
        newFile.setFolderType(existingFile.getFolderType());
        newFile.setStatus(FileStatusEnums.USING.getStatus()); // 转码成功
        newFile.setDelFlag(FileDelFlagEnums.USING.getFlag()); // 正常状态
        newFile.setCreateTime(new Date());
        newFile.setLastUpdateTime(new Date());

        // 插入数据库
        fileInfoMapper.insert(newFile);
        
        log.debug("创建文件引用 - newFileId: {}, sourceFileId: {}, filePath: {}", 
            newFile.getFileId(), existingFile.getFileId(), existingFile.getFilePath());

        return newFile;
    }

    /**
     * 缓存文件 MD5 映射
     * 
     * 在文件上传完成后调用，缓存 MD5 到文件 ID 的映射
     * 
     * @param fileMd5 文件MD5值
     * @param fileId 文件ID
     */
    public void cacheMd5Mapping(String fileMd5, String fileId) {
        if (StringTools.isEmpty(fileMd5) || StringTools.isEmpty(fileId)) {
            log.warn("MD5 或 FileId 为空，跳过缓存 - fileMd5: {}, fileId: {}", fileMd5, fileId);
            return;
        }

        String md5Key = FILE_MD5_KEY + fileMd5;
        redisTemplate.opsForValue().set(md5Key, fileId, MD5_CACHE_TTL_DAYS, TimeUnit.DAYS);
        
        log.debug("缓存 MD5 映射 - fileMd5: {}, fileId: {}", fileMd5, fileId);
    }

    /**
     * 清除 MD5 缓存
     * 
     * 在文件被删除时调用，清除 MD5 映射缓存
     * 
     * @param fileMd5 文件MD5值
     */
    public void clearMd5Cache(String fileMd5) {
        if (StringTools.isEmpty(fileMd5)) {
            return;
        }

        String md5Key = FILE_MD5_KEY + fileMd5;
        redisTemplate.delete(md5Key);
        
        log.debug("清除 MD5 缓存 - fileMd5: {}", fileMd5);
    }
}
