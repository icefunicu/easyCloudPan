package com.easypan.service;

import com.easypan.entity.po.FileInfo;
import com.easypan.mappers.FileInfoMapper;
import com.easypan.strategy.StorageStrategy;
import jakarta.annotation.Resource;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 文件操作服务，使用虚拟线程优化.
 */
@Service
public class FileOperationService {

    private static final Logger logger = LoggerFactory.getLogger(FileOperationService.class);

    @Resource
    @Qualifier("virtualThreadExecutor")
    private AsyncTaskExecutor virtualThreadExecutor;

    @Resource
    private FileInfoService fileInfoService;

    @Resource
    private FileInfoMapper fileInfoMapper;

    @Resource
    @Qualifier("storageFailoverService")
    private StorageStrategy storageStrategy;

    /**
     * 批量删除文件（移入回收站）.
     *
     * @param fileIds 文件ID列表
     * @param userId  用户ID
     */
    public void batchDeleteFile(List<String> fileIds, String userId) {
        CompletableFuture.runAsync(() -> {
            try {
                String fileIdsStr = String.join(",", fileIds);
                logger.info("Virtual Thread: Batch moving files to recycle bin: {}", fileIdsStr);
                fileInfoService.removeFile2RecycleBatch(userId, fileIdsStr);
            } catch (Exception e) {
                logger.error("Failed to batch delete files", e);
            }
        }, virtualThreadExecutor);
    }

    /**
     * 批量下载文件并打包.
     *
     * @param fileIds      文件ID列表
     * @param outputStream 输出流
     * @throws IOException IO异常
     */
    public void downloadMultipleFiles(List<String> fileIds, OutputStream outputStream) throws IOException {
        // 使用 ReentrantLock 替代 synchronized，避免虚拟线程 Pinning
        final ReentrantLock zipLock = new ReentrantLock();

        try (ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {
            List<CompletableFuture<Void>> futures = fileIds.stream()
                    .map(fileId -> CompletableFuture.runAsync(() -> {
                        try {
                            FileInfo fileInfo = fileInfoMapper.selectOneById(fileId);
                            if (fileInfo != null) {
                                try (InputStream inputStream = storageStrategy.download(fileInfo.getFilePath())) {
                                    zipLock.lock();
                                    try {
                                        zipOut.putNextEntry(new ZipEntry(fileInfo.getFileName()));
                                        IOUtils.copy(inputStream, zipOut);
                                        zipOut.closeEntry();
                                    } finally {
                                        zipLock.unlock();
                                    }
                                }
                            }
                        } catch (IOException e) {
                            logger.error("文件下载失败: {}", fileId, e);
                        }
                    }, virtualThreadExecutor))
                    .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }
    }
}
