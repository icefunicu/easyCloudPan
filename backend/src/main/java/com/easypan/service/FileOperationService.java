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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 文件操作服务 - 使用虚拟线程优化
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
     * 批量删除文件 (移入回收站)
     */
    public void batchDeleteFile(List<String> fileIds, String userId) {
        // 直接调用 FileInfoService 的批量删除逻辑
        // FileInfoService.removeFile2RecycleBatch 内部暂未并行化 (因涉及 DB 批量更新)，
        // 若需物理删除优化，请调用 delFileBatch

        // 这里演示异步调用，提高响应速度
        CompletableFuture.runAsync(() -> {
            try {
                // Convert List to comma-separated string required by service
                String fileIdsStr = String.join(",", fileIds);
                logger.info("Virtual Thread: Batch moving files to recycle bin: {}", fileIdsStr);
                fileInfoService.removeFile2RecycleBatch(userId, fileIdsStr);
            } catch (Exception e) {
                logger.error("Failed to batch delete files", e);
            }
        }, virtualThreadExecutor);
    }

    /**
     * 批量下载文件并打包
     */
    public void downloadMultipleFiles(List<String> fileIds, OutputStream outputStream) throws IOException {
        try (ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {
            List<CompletableFuture<Void>> futures = fileIds.stream()
                .map(fileId -> CompletableFuture.runAsync(() -> {
                    try {
                        FileInfo fileInfo = fileInfoMapper.selectOneById(fileId);
                        if (fileInfo != null) {
                            try (InputStream inputStream = storageStrategy.download(fileInfo.getFilePath())) {
                                synchronized (zipOut) {
                                    zipOut.putNextEntry(new ZipEntry(fileInfo.getFileName()));
                                    IOUtils.copy(inputStream, zipOut);
                                    zipOut.closeEntry();
                                }
                            }
                        }
                    } catch (IOException e) {
                        logger.error("文件下载失败: {}", fileId, e);
                    }
                }, virtualThreadExecutor))
                .toList();

            // 等待所有下载完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }
    }
}
