package com.easypan.component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import jakarta.annotation.Resource;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
@Slf4j
public class S3Component {

    @Resource
    private S3Client s3Client;

    @Value("${minio.bucketName}")
    private String bucketName;

    /**
     * 上传文件
     */
    public void uploadFile(String key, File file) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        s3Client.putObject(putObjectRequest, RequestBody.fromFile(file));
    }

    /**
     * 上传字节数组
     */
    public void uploadBytes(String key, byte[] content) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(content));
    }

    /**
     * 下载到本地文件
     */
    public void downloadFile(String key, Path path) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        s3Client.getObject(getObjectRequest, path);
    }

    /**
     * 删除文件
     */
    public void deleteFile(String key) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        s3Client.deleteObject(deleteObjectRequest);
    }

    /**
     * 获取文件输入流
     */
    public java.io.InputStream getInputStream(String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        return s3Client.getObject(getObjectRequest);
    }

    /**
     * 判断对象是否存在
     */
    public boolean exists(String key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            s3Client.headObject(headObjectRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    /**
     * 批量上传目录下的文件
     */
    public void uploadDirectory(String keyPrefix, File directory) {
        if (!directory.exists() || !directory.isDirectory()) {
            return;
        }
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                uploadDirectory(keyPrefix + "/" + file.getName(), file);
            } else {
                uploadFile(keyPrefix + "/" + file.getName(), file);
            }
        }
    }

    /**
     * 递归删除 S3 目录 (按前缀删除)
     */
    public void deleteDirectory(String prefix) {
        String continuationToken = null;
        do {
            ListObjectsV2Request.Builder listReqBuilder = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix);

            if (continuationToken != null) {
                listReqBuilder.continuationToken(continuationToken);
            }

            ListObjectsV2Response listRes = s3Client.listObjectsV2(listReqBuilder.build());

            if (listRes.contents().isEmpty()) {
                break;
            }

            // Batch delete
            List<ObjectIdentifier> objectsToDelete = listRes.contents().stream()
                    .map(s3Object -> ObjectIdentifier.builder().key(s3Object.key()).build())
                    .collect(Collectors.toList());

            if (!objectsToDelete.isEmpty()) {
                Delete objects = Delete.builder()
                        .objects(objectsToDelete)
                        .quiet(true)
                        .build();

                DeleteObjectsRequest deleteReq = DeleteObjectsRequest.builder()
                        .bucket(bucketName)
                        .delete(objects)
                        .build();

                s3Client.deleteObjects(deleteReq);
            }

            continuationToken = listRes.nextContinuationToken();

        } while (continuationToken != null);
    }

    // ==================== 异步方法 (使用 Virtual Threads) ====================

    /**
     * 异步上传文件到 S3
     * 使用 @Async 注解在 Virtual Thread 上执行
     *
     * @param key  S3 对象键
     * @param file 要上传的文件
     * @return CompletableFuture<Void> 异步操作结果
     */
    @Async
    public CompletableFuture<Void> uploadFileAsync(String key, File file) {
        try {
            log.debug("异步上传文件到 S3: {} 在线程: {}", key, Thread.currentThread().getName());
            uploadFile(key, file);
            log.info("文件上传成功: {}", key);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("文件上传失败: {}", key, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * 异步从 S3 下载文件
     * 使用 @Async 注解在 Virtual Thread 上执行
     *
     * @param key  S3 对象键
     * @param path 下载到的本地路径
     * @return CompletableFuture<Void> 异步操作结果
     */
    @Async
    public CompletableFuture<Void> downloadFileAsync(String key, Path path) {
        try {
            log.debug("异步从 S3 下载文件: {} 在线程: {}", key, Thread.currentThread().getName());
            downloadFile(key, path);
            log.info("文件下载成功: {}", key);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("文件下载失败: {}", key, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * 异步从 S3 删除文件
     * 使用 @Async 注解在 Virtual Thread 上执行
     *
     * @param key S3 对象键
     * @return CompletableFuture<Void> 异步操作结果
     */
    @Async
    public CompletableFuture<Void> deleteFileAsync(String key) {
        try {
            log.debug("异步从 S3 删除文件: {} 在线程: {}", key, Thread.currentThread().getName());
            deleteFile(key);
            log.info("文件删除成功: {}", key);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("文件删除失败: {}", key, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * 异步批量删除 S3 目录
     * 使用 @Async 注解在 Virtual Thread 上执行
     *
     * @param prefix S3 对象键前缀
     * @return CompletableFuture<Void> 异步操作结果
     */
    @Async
    public CompletableFuture<Void> deleteDirectoryAsync(String prefix) {
        try {
            log.debug("异步从 S3 删除目录: {} 在线程: {}", prefix, Thread.currentThread().getName());
            deleteDirectory(prefix);
            log.info("目录删除成功: {}", prefix);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("目录删除失败: {}", prefix, e);
            return CompletableFuture.failedFuture(e);
        }
    }
}
