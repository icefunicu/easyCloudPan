package com.easypan.service.impl;

import com.easypan.component.RedisComponent;
import com.easypan.entity.config.AppConfig;
import com.easypan.entity.constants.Constants;
import com.easypan.entity.dto.SessionWebUserDto;
import com.easypan.entity.dto.UploadResultDto;
import com.easypan.entity.dto.UserSpaceDto;
import com.easypan.entity.enums.*;
import com.easypan.entity.po.FileInfo;
import com.easypan.entity.po.UserInfo;
import com.easypan.entity.query.FileInfoQuery;
import com.easypan.entity.query.SimplePage;
import com.easypan.entity.vo.PaginationResultVO;
import com.easypan.exception.BusinessException;
import com.easypan.mappers.FileInfoMapper;
import com.easypan.mappers.UserInfoMapper;
import com.easypan.service.FileInfoService;
import com.easypan.service.MediaTranscodeService;
import com.easypan.utils.DateUtil;
import com.easypan.utils.StringTools;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.AsyncTaskExecutor;
import java.util.concurrent.CompletableFuture;

import jakarta.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 文件信息 业务接口实现
 */
@Service("fileInfoService")
public class FileInfoServiceImpl implements FileInfoService {

    private static final Logger logger = LoggerFactory.getLogger(FileInfoServiceImpl.class);

    @Resource
    @Lazy
    private FileInfoServiceImpl fileInfoService;

    @Resource
    private AppConfig appConfig;

    @Resource
    private FileInfoMapper fileInfoMapper;

    @Resource
    private UserInfoMapper userInfoMapper;

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private com.easypan.component.UploadRateLimiter uploadRateLimiter;

    @Resource
    @Qualifier("virtualThreadExecutor")
    private AsyncTaskExecutor virtualThreadExecutor;

    @Resource
    private MediaTranscodeService mediaTranscodeService;

    @Resource
    @Qualifier("storageFailoverService")
    private com.easypan.strategy.StorageStrategy storageStrategy;

    @Resource
    private com.easypan.service.TenantQuotaService tenantQuotaService;

    @Value("${app.storage.type:local}")
    private String storageType;

    /**
     * 根据条件查询列表
     */
    @Override
    public List<FileInfo> findListByParam(FileInfoQuery param) {
        return this.fileInfoMapper.selectList(param);
    }

    /**
     * 根据条件查询列表
     */
    @Override
    public Integer findCountByParam(FileInfoQuery param) {
        return this.fileInfoMapper.selectCount(param);
    }

    /**
     * 分页查询方法
     */
    @Override
    public PaginationResultVO<FileInfo> findListByPage(FileInfoQuery param) {
        int count = this.findCountByParam(param);
        int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

        SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
        param.setSimplePage(page);
        List<FileInfo> list = this.findListByParam(param);
        PaginationResultVO<FileInfo> result = new PaginationResultVO<>(count, page.getPageSize(), page.getPageNo(),
                page.getPageTotal(), list);
        return result;
    }

    /**
     * 游标分页查询（性能优于 OFFSET 分页）
     * 使用 (create_time, file_id) 作为复合游标
     */
    @Override
    public com.easypan.entity.query.CursorPage<FileInfo> findListByCursor(String userId, String cursor, Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            pageSize = 20;
        }
        if (pageSize > 100) {
            pageSize = 100;
        }
        
        Date cursorTime = null;
        String cursorId = null;
        
        if (cursor != null && !cursor.isEmpty()) {
            String[] parts = cursor.split("_");
            if (parts.length >= 2) {
                try {
                    cursorTime = new Date(Long.parseLong(parts[0]));
                    cursorId = parts[1];
                } catch (NumberFormatException e) {
                    logger.warn("Invalid cursor format: {}", cursor);
                }
            }
        }
        
        int fetchSize = pageSize + 1;
        List<FileInfo> list;
        
        if (cursorTime == null || cursorId == null) {
            FileInfoQuery query = new FileInfoQuery();
            query.setUserId(userId);
            query.setOrderBy("create_time desc, file_id desc");
            SimplePage page = new SimplePage(0, fetchSize);
            query.setSimplePage(page);
            list = this.findListByParam(query);
        } else {
            list = this.fileInfoMapper.selectByCursorPagination(userId, cursorTime, cursorId, fetchSize);
        }
        
        String nextCursor = null;
        if (list.size() > pageSize) {
            FileInfo lastItem = list.get(pageSize - 1);
            nextCursor = lastItem.getCreateTime().getTime() + "_" + lastItem.getFileId();
            list = list.subList(0, pageSize);
        }
        
        return com.easypan.entity.query.CursorPage.of(list, nextCursor, pageSize);
    }

    /**
     * 新增
     */
    @Override
    public Integer add(FileInfo bean) {
        return this.fileInfoMapper.insert(bean);
    }

    /**
     * 批量新增
     */
    @Override
    public Integer addBatch(List<FileInfo> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.fileInfoMapper.insertBatch(listBean);
    }

    /**
     * 批量新增或者修改
     */
    @Override
    public Integer addOrUpdateBatch(List<FileInfo> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.fileInfoMapper.insertOrUpdateBatch(listBean);
    }

    /**
     * 根据FileIdAndUserId获取对象
     */
    @Override
    public FileInfo getFileInfoByFileIdAndUserId(String fileId, String userId) {
        return this.fileInfoMapper.selectByFileIdAndUserId(fileId, userId);
    }

    /**
     * 根据FileIdAndUserId修改
     */
    @Override
    public Integer updateFileInfoByFileIdAndUserId(FileInfo bean, String fileId, String userId) {
        return this.fileInfoMapper.updateByFileIdAndUserId(bean, fileId, userId);
    }

    /**
     * 根据FileIdAndUserId删除
     */
    @Override
    public Integer deleteFileInfoByFileIdAndUserId(String fileId, String userId) {
        return this.fileInfoMapper.deleteByFileIdAndUserId(fileId, userId);
    }

    /**
     * 上传文件
     */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UploadResultDto uploadFile(SessionWebUserDto webUserDto, String fileId, MultipartFile file, String fileName,
            String filePid, String fileMd5,
            Integer chunkIndex, Integer chunks) {

        // 基本分片参数校验
        if (chunkIndex == null || chunks == null || chunkIndex < 0 || chunks <= 0 || chunkIndex >= chunks) {
            throw new BusinessException("非法的分片参数");
        }

        // 上传并发控制：为每个用户限制同时上传任务数量
        if (!uploadRateLimiter.tryAcquire(webUserDto.getUserId())) {
            throw new BusinessException("当前上传请求过多，请稍后重试");
        }

        // Check tenant storage quota
        tenantQuotaService.checkStorageQuota(file.getSize());
        
        File tempFileFolder = null;
        Boolean uploadSuccess = true;
        try {
            // 文件类型安全校验（第一个分片时检查）
            if (chunkIndex == 0) {
                String fileSuffix = StringTools.getFileSuffix(fileName);

                // 检查是否为危险文件类型
                if (com.easypan.utils.FileTypeValidator.isDangerousFileType(fileSuffix)) {
                    throw new BusinessException("不允许上传可执行文件类型");
                }

                // 验证文件真实类型
                try (InputStream inputStream = file.getInputStream()) {
                    if (!com.easypan.utils.FileTypeValidator.validateFileType(inputStream, fileSuffix)) {
                        logger.warn("File type validation failed: fileName={}, suffix={}", fileName, fileSuffix);
                        throw new BusinessException("文件类型不匹配，请上传正确的文件");
                    }
                } catch (IOException e) {
                    logger.error("Error validating file type", e);
                    throw new BusinessException("文件类型校验失败");
                }
            }

            UploadResultDto resultDto = new UploadResultDto();
            if (StringTools.isEmpty(fileId)) {
                fileId = StringTools.getRandomString(Constants.LENGTH_10);
            }
            resultDto.setFileId(fileId);
            Date curDate = new Date();
            UserSpaceDto spaceDto = redisComponent.getUserSpaceUse(webUserDto.getUserId());
            if (chunkIndex == 0) {
                // 首个分片，先通过布隆过滤器快速判断是否有可能命中秒传
                FileInfo dbFile = null;
                if (!StringTools.isEmpty(fileMd5) && redisComponent.mightContainFileMd5(fileMd5)) {
                    // 使用专用索引方法进行秒传查询，避免不必要的全表扫描
                    dbFile = this.fileInfoMapper.selectOneByMd5AndStatus(
                            fileMd5, FileStatusEnums.USING.getStatus());
                }
                // 秒传
                if (dbFile != null) {
                    // 判断文件状态
                    if (dbFile.getFileSize() + spaceDto.getUseSpace() > spaceDto.getTotalSpace()) {
                        throw new BusinessException(ResponseCodeEnum.CODE_904);
                    }
                    dbFile.setFileId(fileId);
                    dbFile.setFilePid(filePid);
                    dbFile.setUserId(webUserDto.getUserId());
                    dbFile.setFileMd5(null);
                    dbFile.setCreateTime(curDate);
                    dbFile.setLastUpdateTime(curDate);
                    dbFile.setStatus(FileStatusEnums.USING.getStatus());
                    dbFile.setDelFlag(FileDelFlagEnums.USING.getFlag());
                    dbFile.setFileMd5(fileMd5);
                    fileName = autoRename(filePid, webUserDto.getUserId(), fileName);
                    dbFile.setFileName(fileName);
                    this.fileInfoMapper.insert(dbFile);
                    resultDto.setStatus(UploadStatusEnums.UPLOAD_SECONDS.getCode());
                    // 更新用户空间使用
                    updateUserSpace(webUserDto, dbFile.getFileSize());

                    return resultDto;
                }
            }
            // 暂存在临时目录
            String tempFolderName = appConfig.getProjectFolder() + Constants.FILE_FOLDER_TEMP;
            String currentUserFolderName = webUserDto.getUserId() + fileId;
            // 创建临时目录
            tempFileFolder = new File(tempFolderName + currentUserFolderName);
            if (!tempFileFolder.exists()) {
                tempFileFolder.mkdirs();
            }

            // 判断磁盘空间
            Long currentTempSize = redisComponent.getFileTempSize(webUserDto.getUserId(), fileId);
            if (file.getSize() + currentTempSize + spaceDto.getUseSpace() > spaceDto.getTotalSpace()) {
                throw new BusinessException(ResponseCodeEnum.CODE_904);
            }

            File newFile = new File(tempFileFolder.getPath() + "/" + chunkIndex);

            // 如果分片已经存在且大小一致，则认为已上传成功，直接跳过写入与空间累加（支持断点续传幂等）
            if (!(newFile.exists() && newFile.length() == file.getSize())) {
                file.transferTo(newFile);

                // 简单分片校验：确认写入后的大小与请求分片大小一致
                if (newFile.length() != file.getSize()) {
                    throw new BusinessException("分片大小校验失败，请重试上传");
                }

                // 保存临时大小（仅在实际写入新分片时累加）
                redisComponent.saveFileTempSize(webUserDto.getUserId(), fileId, file.getSize());
            }
            // 不是最后一个分片，直接返回
            if (chunkIndex < chunks - 1) {
                resultDto.setStatus(UploadStatusEnums.UPLOADING.getCode());
                return resultDto;
            }
            // 最后一个分片上传完成，记录数据库，异步合并分片
            String month = DateUtil.format(curDate, DateTimePatternEnum.YYYYMM.getPattern());
            String fileSuffix = StringTools.getFileSuffix(fileName);
            // 真实文件名
            String realFileName = currentUserFolderName + fileSuffix;
            FileTypeEnums fileTypeEnum = FileTypeEnums.getFileTypeBySuffix(fileSuffix);
            // 自动重命名
            fileName = autoRename(filePid, webUserDto.getUserId(), fileName);
            FileInfo fileInfo = new FileInfo();
            fileInfo.setFileId(fileId);
            fileInfo.setUserId(webUserDto.getUserId());
            fileInfo.setFileMd5(fileMd5);
            fileInfo.setFileName(fileName);
            fileInfo.setFilePath(month + "/" + realFileName);
            fileInfo.setFilePid(filePid);
            fileInfo.setCreateTime(curDate);
            fileInfo.setLastUpdateTime(curDate);
            fileInfo.setFileCategory(fileTypeEnum.getCategory().getCategory());
            fileInfo.setFileType(fileTypeEnum.getType());
            fileInfo.setStatus(FileStatusEnums.TRANSFER.getStatus());
            fileInfo.setFolderType(FileFolderTypeEnums.FILE.getType());
            fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());
            this.fileInfoMapper.insert(fileInfo);

            // 新文件插入成功后，将 MD5 加入布隆过滤器，提升后续秒传命中率
            if (!StringTools.isEmpty(fileMd5)) {
                redisComponent.addFileMd5ToBloom(fileMd5);
            }

            Long totalSize = redisComponent.getFileTempSize(webUserDto.getUserId(), fileId);
            updateUserSpace(webUserDto, totalSize);

            resultDto.setStatus(UploadStatusEnums.UPLOAD_FINISH.getCode());
            // 事务提交后调用异步方法
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    fileInfoService.transferFile(fileInfo.getFileId(), webUserDto);
                }
            });
            return resultDto;
        } catch (BusinessException e) {
            uploadSuccess = false;
            logger.error("文件上传失败", e);
            throw e;
        } catch (Exception e) {
            uploadSuccess = false;
            logger.error("文件上传失败", e);
            throw new BusinessException("文件上传失败");
        } finally {
            // 释放上传并发许可
            uploadRateLimiter.release(webUserDto.getUserId());

            // 如果上传失败，清除临时目录
            if (tempFileFolder != null && !uploadSuccess) {
                try {
                    FileUtils.deleteDirectory(tempFileFolder);
                } catch (IOException e) {
                    logger.error("删除临时目录失败");
                }
            }
        }
    }

    /**
     * 更新用户空间使用
     */
    private void updateUserSpace(SessionWebUserDto webUserDto, Long totalSize) {
        Integer count = userInfoMapper.updateUserSpace(webUserDto.getUserId(), totalSize, null);
        if (count == 0) {
            throw new BusinessException(ResponseCodeEnum.CODE_904);
        }
        UserSpaceDto spaceDto = redisComponent.getUserSpaceUse(webUserDto.getUserId());
        spaceDto.setUseSpace(spaceDto.getUseSpace() + totalSize);
        redisComponent.saveUserSpaceUse(webUserDto.getUserId(), spaceDto);
    }

    /**
     * 自动重命名
     */
    private String autoRename(String filePid, String userId, String fileName) {
        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setUserId(userId);
        fileInfoQuery.setFilePid(filePid);
        fileInfoQuery.setDelFlag(FileDelFlagEnums.USING.getFlag());
        fileInfoQuery.setFileName(fileName);
        Integer count = this.fileInfoMapper.selectCount(fileInfoQuery);
        if (count > 0) {
            return StringTools.rename(fileName);
        }

        return fileName;
    }

    /**
     * 转码
     */
    @Async
    public void transferFile(String fileId, SessionWebUserDto webUserDto) {
        Boolean transferSuccess = true;
        String targetFilePath = null;
        String cover = null;
        FileTypeEnums fileTypeEnum = null;
        FileInfo fileInfo = fileInfoMapper.selectByFileIdAndUserId(fileId, webUserDto.getUserId());
        try {
            if (fileInfo == null || !FileStatusEnums.TRANSFER.getStatus().equals(fileInfo.getStatus())) {
                return;
            }
            // 临时目录
            String tempFolderName = appConfig.getProjectFolder() + Constants.FILE_FOLDER_TEMP;
            String currentUserFolderName = webUserDto.getUserId() + fileId;
            File fileFolder = new File(tempFolderName + currentUserFolderName);
            if (!fileFolder.exists()) {
                fileFolder.mkdirs();
            }
            // 文件后缀
            String fileSuffix = StringTools.getFileSuffix(fileInfo.getFileName());
            String month = DateUtil.format(fileInfo.getCreateTime(), DateTimePatternEnum.YYYYMM.getPattern());
            // 目标目录
            String targetFolderName = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE;
            File targetFolder = new File(targetFolderName + "/" + month);
            if (!targetFolder.exists()) {
                targetFolder.mkdirs();
            }
            // 真实文件名
            String realFileName = currentUserFolderName + fileSuffix;
            // 真实文件路径
            targetFilePath = targetFolder.getPath() + "/" + realFileName;
            // 使用 NIO 方式合并文件，减少内存占用
            unionWithNIO(fileFolder.getPath(), targetFilePath, fileInfo.getFileName(), true);

            // 上传原始文件到存储后端
            com.easypan.strategy.StorageStrategy storageStrategy = this.storageStrategy;
            storageStrategy.upload(new File(targetFilePath), fileInfo.getFilePath());

            // 视频文件切割
            fileTypeEnum = FileTypeEnums.getFileTypeBySuffix(fileSuffix);
            if (FileTypeEnums.VIDEO == fileTypeEnum) {
                cutFile4Video(fileId, targetFilePath);
                // 视频生成缩略图
                cover = month + "/" + currentUserFolderName + Constants.IMAGE_PNG_SUFFIX;
                String coverPath = targetFolderName + "/" + cover;
                File coverFile = new File(coverPath);
                mediaTranscodeService.createVideoCover(new File(targetFilePath), Constants.LENGTH_150, coverFile);
                // 上传封面到存储后端
                if (coverFile.exists()) {
                    storageStrategy.upload(coverFile, cover);
                }
                // 上传切片目录到存储后端
                String tsFolderName = targetFilePath.substring(0, targetFilePath.lastIndexOf("."));
                File tsFolder = new File(tsFolderName);
                if (tsFolder.exists()) {
                    storageStrategy.uploadDirectory(
                            fileInfo.getFilePath().substring(0, fileInfo.getFilePath().lastIndexOf(".")), tsFolder);
                }
            } else if (FileTypeEnums.IMAGE == fileTypeEnum) {
                // 生成缩略图
                cover = month + "/" + realFileName.replace(".", "_.");
                String coverPath = targetFolderName + "/" + cover;
                File coverFile = new File(coverPath);
                Boolean created = mediaTranscodeService.createThumbnail(new File(targetFilePath), Constants.LENGTH_150,
                        coverFile, false);
                if (!created) {
                    FileUtils.copyFile(new File(targetFilePath), coverFile);
                }
                // 上传封面到存储后端
                storageStrategy.upload(coverFile, cover);
            }
        } catch (Exception e) {
            logger.error("文件转码失败，文件Id:{},userId:{}", fileId, webUserDto.getUserId(), e);
            transferSuccess = false;
        } finally {
            FileInfo updateInfo = new FileInfo();
            File targetFile = new File(targetFilePath);
            updateInfo.setFileSize(targetFile.exists() ? targetFile.length() : 0L);
            updateInfo.setFileCover(cover);
            updateInfo.setStatus(
                    transferSuccess ? FileStatusEnums.USING.getStatus() : FileStatusEnums.TRANSFER_FAIL.getStatus());
            fileInfoMapper.updateFileStatusWithOldStatus(fileId, webUserDto.getUserId(), updateInfo,
                    FileStatusEnums.TRANSFER.getStatus());

            // 清理本地文件
            // 只有当存储类型不是 LOCAL 时才删除本地文件
            if (targetFilePath != null
                    && !com.easypan.entity.enums.StorageTypeEnum.LOCAL.getCode().equals(storageType)) {
                FileUtils.deleteQuietly(new File(targetFilePath));
                String tsFolderName = targetFilePath.substring(0, targetFilePath.lastIndexOf("."));
                FileUtils.deleteQuietly(new File(tsFolderName));
            }
        }
    }

    /**
     * 合并文件（legacy 实现，保留以兼容旧逻辑）
     */
    public static void union(String dirPath, String toFilePath, String fileName, boolean delSource)
            throws BusinessException {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            throw new BusinessException("目录不存在");
        }
        // 获取目录下的所有文件列表
        File fileList[] = dir.listFiles();
        // 创建目标文件对象
        File targetFile = new File(toFilePath);
        RandomAccessFile writeFile = null;
        try {
            // 创建随机访问文件对象用于写入
            writeFile = new RandomAccessFile(targetFile, "rw");
            // 优化：增大缓冲区到 1MB，提升 I/O 性能
            byte[] b = new byte[1024 * 1024];
            // 遍历目录下的文件列表
            for (int i = 0; i < fileList.length; i++) {
                int len = -1;
                // 创建读取块文件的随机访问文件对象
                File chunkFile = new File(dirPath + File.separator + i);
                RandomAccessFile readFile = null;
                try {
                    // 创建随机访问文件对象用于读取
                    readFile = new RandomAccessFile(chunkFile, "r");
                    // 读取块文件的内容，并写入目标文件
                    while ((len = readFile.read(b)) != -1) {
                        writeFile.write(b, 0, len);
                    }
                } catch (Exception e) {
                    logger.error("合并分片失败", e);
                    throw new BusinessException("合并文件失败");
                } finally {
                    if (readFile != null) {
                        readFile.close();
                    }
                }
            }
        } catch (Exception e) {
            logger.error("合并文件:{}失败", fileName, e);
            throw new BusinessException("合并文件" + fileName + "出错了");
        } finally {
            try {
                if (null != writeFile) {
                    writeFile.close();
                }
            } catch (IOException e) {
                logger.error("关闭流失败", e);
            }
            // 根据参数决定是否删除原始文件夹
            if (delSource) {
                if (dir.exists()) {
                    try {
                        FileUtils.deleteDirectory(dir);
                    } catch (IOException e) {
                        logger.error("删除临时目录失败: {}", dir.getPath(), e);
                    }
                }
            }
        }
    }

    /**
     * 使用 NIO FileChannel 零拷贝方式合并分片文件，降低内存占用并提升 I/O 性能。
     *
     * 对应任务：
     *  - 6.1.1 创建 NIO 合并方法
     */
    private static void unionWithNIO(String dirPath, String toFilePath, String fileName, boolean delSource)
            throws BusinessException {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            throw new BusinessException("目录不存在");
        }

        File[] chunks = dir.listFiles();
        if (chunks == null || chunks.length == 0) {
            throw new BusinessException("未找到分片文件");
        }

        // 按文件名排序，确保按分片顺序合并
        Arrays.sort(chunks, Comparator.comparing(File::getName));

        java.nio.file.Path targetPath = java.nio.file.Paths.get(toFilePath);
        try (java.nio.channels.FileChannel outChannel = java.nio.channels.FileChannel.open(
                targetPath,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.WRITE,
                java.nio.file.StandardOpenOption.TRUNCATE_EXISTING)) {

            for (File chunk : chunks) {
                try (java.nio.channels.FileChannel inChannel = java.nio.channels.FileChannel.open(
                        chunk.toPath(),
                        java.nio.file.StandardOpenOption.READ)) {
                    long size = inChannel.size();
                    long position = 0L;
                    while (position < size) {
                        long transferred = inChannel.transferTo(position, size - position, outChannel);
                        if (transferred <= 0) {
                            break;
                        }
                        position += transferred;
                    }
                } catch (Exception e) {
                    logger.error("NIO 合并分片失败", e);
                    throw new BusinessException("合并文件失败");
                }
            }
        } catch (Exception e) {
            logger.error("NIO 合并文件:{}失败", fileName, e);
            throw new BusinessException("合并文件" + fileName + "出错了");
        } finally {
            if (delSource && dir.exists()) {
                try {
                    FileUtils.deleteDirectory(dir);
                } catch (IOException e) {
                    logger.error("删除临时目录失败: {}", dir.getPath(), e);
                }
            }
        }
    }

    /**
     * 切割视频文件
     */
    private void cutFile4Video(String fileId, String videoFilePath) {
        // 创建同名切片目录
        File tsFolder = new File(videoFilePath.substring(0, videoFilePath.lastIndexOf(".")));
        if (!tsFolder.exists()) {
            tsFolder.mkdirs();
        }

        String tsPath = tsFolder + "/" + Constants.TS_NAME;
        // 生成.ts
        mediaTranscodeService.transcodeToTs(videoFilePath, tsPath);
        // 生成索引文件.m3u8 和切片.ts
        mediaTranscodeService.cutToM3u8(tsPath, tsFolder.getPath(), fileId);
        // 删除index.ts
        new File(tsPath).delete();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileInfo rename(String fileId, String userId, String fileName) {
        FileInfo fileInfo = this.fileInfoMapper.selectByFileIdAndUserId(fileId, userId);
        if (fileInfo == null) {
            throw new BusinessException("文件不存在");
        }
        if (fileInfo.getFileName().equals(fileName)) {
            return fileInfo;
        }
        String filePid = fileInfo.getFilePid();
        checkFileName(filePid, userId, fileName, fileInfo.getFolderType());
        // 文件获取后缀
        if (FileFolderTypeEnums.FILE.getType().equals(fileInfo.getFolderType())) {
            fileName = fileName + StringTools.getFileSuffix(fileInfo.getFileName());
        }
        Date curDate = new Date();
        FileInfo dbInfo = new FileInfo();
        dbInfo.setFileName(fileName);
        dbInfo.setLastUpdateTime(curDate);
        this.fileInfoMapper.updateByFileIdAndUserId(dbInfo, fileId, userId);

        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setFilePid(filePid);
        fileInfoQuery.setUserId(userId);
        fileInfoQuery.setFileName(fileName);
        fileInfoQuery.setDelFlag(FileDelFlagEnums.USING.getFlag());
        Integer count = this.fileInfoMapper.selectCount(fileInfoQuery);
        if (count > 1) {
            throw new BusinessException("文件名" + fileName + "已经存在");
        }
        fileInfo.setFileName(fileName);
        fileInfo.setLastUpdateTime(curDate);
        return fileInfo;
    }

    private void checkFileName(String filePid, String userId, String fileName, Integer folderType) {
        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setFolderType(folderType);
        fileInfoQuery.setFileName(fileName);
        fileInfoQuery.setFilePid(filePid);
        fileInfoQuery.setUserId(userId);
        fileInfoQuery.setDelFlag(FileDelFlagEnums.USING.getFlag());
        Integer count = this.fileInfoMapper.selectCount(fileInfoQuery);
        if (count > 0) {
            throw new BusinessException("此目录下已存在同名文件，请修改名称");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileInfo newFolder(String filePid, String userId, String folderName) {
        checkFileName(filePid, userId, folderName, FileFolderTypeEnums.FOLDER.getType());
        Date curDate = new Date();
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileId(StringTools.getRandomString(Constants.LENGTH_10));
        fileInfo.setUserId(userId);
        fileInfo.setFilePid(filePid);
        fileInfo.setFileName(folderName);
        fileInfo.setFolderType(FileFolderTypeEnums.FOLDER.getType());
        fileInfo.setCreateTime(curDate);
        fileInfo.setLastUpdateTime(curDate);
        fileInfo.setStatus(FileStatusEnums.USING.getStatus());
        fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());
        this.fileInfoMapper.insert(fileInfo);

        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setFilePid(filePid);
        fileInfoQuery.setUserId(userId);
        fileInfoQuery.setFileName(folderName);
        fileInfoQuery.setFolderType(FileFolderTypeEnums.FOLDER.getType());
        fileInfoQuery.setDelFlag(FileDelFlagEnums.USING.getFlag());
        Integer count = this.fileInfoMapper.selectCount(fileInfoQuery);
        if (count > 1) {
            throw new BusinessException("文件夹" + folderName + "已经存在");
        }
        fileInfo.setFileName(folderName);
        fileInfo.setLastUpdateTime(curDate);
        return fileInfo;
    }

    @Transactional(rollbackFor = Exception.class)
    public void changeFileFolder(String fileIds, String filePid, String userId) {
        if (fileIds.equals(filePid)) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (!Constants.ZERO_STR.equals(filePid)) {
            FileInfo fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(filePid, userId);
            if (fileInfo == null || !FileDelFlagEnums.USING.getFlag().equals(fileInfo.getDelFlag())) {
                throw new BusinessException(ResponseCodeEnum.CODE_600);
            }
        }
        String[] fileIdArray = fileIds.split(",");

        FileInfoQuery query = new FileInfoQuery();
        query.setFilePid(filePid);
        query.setUserId(userId);
        List<FileInfo> dbFileList = fileInfoService.findListByParam(query);

        Map<String, FileInfo> dbFileNameMap = dbFileList.stream()
                .collect(Collectors.toMap(FileInfo::getFileName, Function.identity(), (file1, file2) -> file2));
        // 查询选中的文件
        query = new FileInfoQuery();
        query.setUserId(userId);
        query.setFileIdArray(fileIdArray);
        List<FileInfo> selectFileList = fileInfoService.findListByParam(query);

        // 将所选文件重命名
        for (FileInfo item : selectFileList) {
            FileInfo rootFileInfo = dbFileNameMap.get(item.getFileName());
            // 文件名已经存在，重命名被还原的文件名
            FileInfo updateInfo = new FileInfo();
            if (rootFileInfo != null) {
                String fileName = StringTools.rename(item.getFileName());
                updateInfo.setFileName(fileName);
            }
            updateInfo.setFilePid(filePid);
            this.fileInfoMapper.updateByFileIdAndUserId(updateInfo, item.getFileId(), userId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeFile2RecycleBatch(String userId, String fileIds) {
        String[] fileIdArray = fileIds.split(",");
        FileInfoQuery query = new FileInfoQuery();
        query.setUserId(userId);
        query.setFileIdArray(fileIdArray);
        query.setDelFlag(FileDelFlagEnums.USING.getFlag());
        List<FileInfo> fileInfoList = this.fileInfoMapper.selectList(query);
        if (fileInfoList.isEmpty()) {
            return;
        }
        List<String> delFilePidList = new ArrayList<>();
        List<String> folderIds = fileInfoList.stream()
                .filter(item -> FileFolderTypeEnums.FOLDER.getType().equals(item.getFolderType()))
                .map(FileInfo::getFileId)
                .collect(Collectors.toList());
        if (!folderIds.isEmpty()) {
            delFilePidList = this.fileInfoMapper.selectDescendantFolderIds(folderIds, userId,
                    FileDelFlagEnums.USING.getFlag());
        }

        // 将目录下的所有文件更新为已删除
        if (!delFilePidList.isEmpty()) {
            FileInfo updateInfo = new FileInfo();
            updateInfo.setDelFlag(FileDelFlagEnums.DEL.getFlag());
            this.fileInfoMapper.updateFileDelFlagBatch(updateInfo, userId, delFilePidList, null,
                    FileDelFlagEnums.USING.getFlag());
        }

        // 将选中的文件更新为回收站
        List<String> delFileIdList = Arrays.asList(fileIdArray);
        FileInfo fileInfo = new FileInfo();
        fileInfo.setRecoveryTime(new Date());
        fileInfo.setDelFlag(FileDelFlagEnums.RECYCLE.getFlag());
        this.fileInfoMapper.updateFileDelFlagBatch(fileInfo, userId, null, delFileIdList,
                FileDelFlagEnums.USING.getFlag());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recoverFileBatch(String userId, String fileIds) {
        String[] fileIdArray = fileIds.split(",");
        FileInfoQuery query = new FileInfoQuery();
        query.setUserId(userId);
        query.setFileIdArray(fileIdArray);
        query.setDelFlag(FileDelFlagEnums.RECYCLE.getFlag());
        List<FileInfo> fileInfoList = this.fileInfoMapper.selectList(query);

        List<String> delFileSubFolderFileIdList = new ArrayList<>();
        // 找到所选文件子目录文件ID
        List<String> folderIds = fileInfoList.stream()
                .filter(item -> FileFolderTypeEnums.FOLDER.getType().equals(item.getFolderType()))
                .map(FileInfo::getFileId)
                .collect(Collectors.toList());
        if (!folderIds.isEmpty()) {
            delFileSubFolderFileIdList = this.fileInfoMapper.selectDescendantFolderIds(folderIds, userId,
                    FileDelFlagEnums.DEL.getFlag());
        }
        // 查询所有根目录的文件
        query = new FileInfoQuery();
        query.setUserId(userId);
        query.setDelFlag(FileDelFlagEnums.USING.getFlag());
        query.setFilePid(Constants.ZERO_STR);
        List<FileInfo> allRootFileList = this.fileInfoMapper.selectList(query);

        Map<String, FileInfo> rootFileMap = allRootFileList.stream()
                .collect(Collectors.toMap(FileInfo::getFileName, Function.identity(), (file1, file2) -> file2));

        // 查询所有所选文件
        // 将目录下的所有删除的文件更新为正常
        if (!delFileSubFolderFileIdList.isEmpty()) {
            FileInfo fileInfo = new FileInfo();
            fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());
            this.fileInfoMapper.updateFileDelFlagBatch(fileInfo, userId, delFileSubFolderFileIdList, null,
                    FileDelFlagEnums.DEL.getFlag());
        }
        // 将选中的文件更新为正常,且父级目录到跟目录
        List<String> delFileIdList = Arrays.asList(fileIdArray);
        FileInfo fileInfo = new FileInfo();
        fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());
        fileInfo.setFilePid(Constants.ZERO_STR);
        fileInfo.setLastUpdateTime(new Date());
        this.fileInfoMapper.updateFileDelFlagBatch(fileInfo, userId, null, delFileIdList,
                FileDelFlagEnums.RECYCLE.getFlag());

        // 将所选文件重命名
        for (FileInfo item : fileInfoList) {
            FileInfo rootFileInfo = rootFileMap.get(item.getFileName());
            // 文件名已经存在，重命名被还原的文件名
            if (rootFileInfo != null) {
                String fileName = StringTools.rename(item.getFileName());
                FileInfo updateInfo = new FileInfo();
                updateInfo.setFileName(fileName);
                this.fileInfoMapper.updateByFileIdAndUserId(updateInfo, item.getFileId(), userId);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delFileBatch(String userId, String fileIds, Boolean adminOp) {
        String[] fileIdArray = fileIds.split(",");

        FileInfoQuery query = new FileInfoQuery();
        query.setUserId(userId);
        query.setFileIdArray(fileIdArray);
        if (!adminOp) {
            query.setDelFlag(FileDelFlagEnums.RECYCLE.getFlag());
        }
        List<FileInfo> fileInfoList = this.fileInfoMapper.selectList(query);
        List<String> delFileSubFolderFileIdList = new ArrayList<>();
        // 找到所选文件子目录文件ID
        List<String> folderIds = fileInfoList.stream()
                .filter(item -> FileFolderTypeEnums.FOLDER.getType().equals(item.getFolderType()))
                .map(FileInfo::getFileId)
                .collect(Collectors.toList());
        if (!folderIds.isEmpty()) {
            delFileSubFolderFileIdList = this.fileInfoMapper.selectDescendantFolderIds(folderIds, userId,
                    FileDelFlagEnums.DEL.getFlag());
        }

        // 删除所选文件，子目录中的文件
        if (!delFileSubFolderFileIdList.isEmpty()) {
            this.fileInfoMapper.delFileBatch(userId, delFileSubFolderFileIdList, null,
                    adminOp ? null : FileDelFlagEnums.DEL.getFlag());
        }
        // 删除所选文件
        this.fileInfoMapper.delFileBatch(userId, null, Arrays.asList(fileIdArray),
                adminOp ? null : FileDelFlagEnums.RECYCLE.getFlag());

        Long useSpace = this.fileInfoMapper.selectUseSpace(userId);
        UserInfo userInfo = new UserInfo();
        userInfo.setUseSpace(useSpace);
        this.userInfoMapper.updateByUserId(userInfo, userId);

        // 设置缓存
        UserSpaceDto userSpaceDto = redisComponent.getUserSpaceUse(userId);
        userSpaceDto.setUseSpace(useSpace);
        redisComponent.saveUserSpaceUse(userId, userSpaceDto);

        // 物理删除存储后端文件 (使用虚拟线程并发删除)
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        com.easypan.strategy.StorageStrategy storageStrategy = this.storageStrategy;
        for (FileInfo fileInfo : fileInfoList) {
            if (FileFolderTypeEnums.FILE.getType().equals(fileInfo.getFolderType())) {
                futures.add(CompletableFuture.runAsync(() -> {
                    storageStrategy.delete(fileInfo.getFilePath());
                    if (fileInfo.getFileCover() != null) {
                        storageStrategy.delete(fileInfo.getFileCover());
                    }
                    if (FileTypeEnums.VIDEO.getType().equals(fileInfo.getFileType())) {
                        // 删除切片目录
                        String tsFolderKey = fileInfo.getFilePath().substring(0,
                                fileInfo.getFilePath().lastIndexOf("."));
                        storageStrategy.deleteDirectory(tsFolderKey);
                    }
                }, virtualThreadExecutor).exceptionally(e -> {
                    logger.error("物理删除文件失败: {}", fileInfo.getFilePath(), e);
                    return null;
                }));
            }
        }

        // 等待所有删除操作完成
        if (!futures.isEmpty()) {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }
    }

    @Override
    public void checkRootFilePid(String rootFilePid, String userId, String fileId) {
        if (StringTools.isEmpty(fileId)) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (rootFilePid.equals(fileId)) {
            return;
        }
        checkFilePid(rootFilePid, fileId, userId);
    }

    private void checkFilePid(String rootFilePid, String fileId, String userId) {
        FileInfo fileInfo = this.fileInfoMapper.selectByFileIdAndUserId(fileId, userId);
        if (fileInfo == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (Constants.ZERO_STR.equals(fileInfo.getFilePid())) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (fileInfo.getFilePid().equals(rootFilePid)) {
            return;
        }
        checkFilePid(rootFilePid, fileInfo.getFilePid(), userId);
    }

    @Override
    public void saveShare(String shareRootFilePid, String shareFileIds, String myFolderId, String shareUserId,
            String cureentUserId) {
        String[] shareFileIdArray = shareFileIds.split(",");
        // 目标目录文件列表
        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setUserId(cureentUserId);
        fileInfoQuery.setFilePid(myFolderId);
        List<FileInfo> currentFileList = this.fileInfoMapper.selectList(fileInfoQuery);
        Map<String, FileInfo> currentFileMap = currentFileList.stream()
                .collect(Collectors.toMap(FileInfo::getFileName, Function.identity(), (file1, file2) -> file2));
        // 选择的文件
        fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setUserId(shareUserId);
        fileInfoQuery.setFileIdArray(shareFileIdArray);
        List<FileInfo> shareFileList = this.fileInfoMapper.selectList(fileInfoQuery);
        // 重命名选择的文件
        List<FileInfo> copyFileList = new ArrayList<>();
        Date curDate = new Date();
        for (FileInfo item : shareFileList) {
            FileInfo haveFile = currentFileMap.get(item.getFileName());
            if (haveFile != null) {
                item.setFileName(StringTools.rename(item.getFileName()));
            }
            findAllSubFile(copyFileList, item, shareUserId, cureentUserId, curDate, myFolderId);
        }
        logger.debug("准备批量插入文件数量: {}", copyFileList.size());
        this.fileInfoMapper.insertBatch(copyFileList);
    }

    private void findAllSubFile(List<FileInfo> copyFileList, FileInfo fileInfo, String sourceUserId,
            String currentUserId, Date curDate, String newFilePid) {
        String sourceFileId = fileInfo.getFileId();
        fileInfo.setCreateTime(curDate);
        fileInfo.setLastUpdateTime(curDate);
        fileInfo.setFilePid(newFilePid);
        fileInfo.setUserId(currentUserId);
        String newFileId = StringTools.getRandomString(Constants.LENGTH_10);
        fileInfo.setFileId(newFileId);
        copyFileList.add(fileInfo);
        if (FileFolderTypeEnums.FOLDER.getType().equals(fileInfo.getFolderType())) {
            FileInfoQuery query = new FileInfoQuery();
            query.setFilePid(sourceFileId);
            query.setUserId(sourceUserId);
            List<FileInfo> sourceFileList = this.fileInfoMapper.selectList(query);
            for (FileInfo item : sourceFileList) {
                findAllSubFile(copyFileList, item, sourceUserId, currentUserId, curDate, newFileId);
            }
        }
    }

    @Override
    public Long getUserUseSpace(String userId) {
        return this.fileInfoMapper.selectUseSpace(userId);
    }

    @Override
    public void deleteFileByUserId(String userId) {
        this.fileInfoMapper.deleteFileByUserId(userId);
    }
}