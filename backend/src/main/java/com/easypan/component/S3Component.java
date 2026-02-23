package com.easypan.component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartCopyRequest;
import software.amazon.awssdk.services.s3.model.UploadPartCopyResponse;

import jakarta.annotation.Resource;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * S3 存储操作组件，封装 MinIO/S3 的常用操作.
 */
@Component
@Slf4j
public class S3Component {

    @Resource
    private S3Client s3Client;

    @Resource
    private S3Presigner s3Presigner;

    @Value("${minio.bucketName}")
    private String bucketName;

    /**
     * 生成带时效的预签名下载链接.
     *
     * @param key      S3 对象键
     * @param fileName 下载时客户端指定的文件名
     * @return 预签名URL字符串
     */
    public String generatePresignedUrl(String key, String fileName) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .responseContentDisposition("attachment;filename=\"" + fileName + "\"")
                .build();

        GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(2)) // 2小时有效期
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presignedGetObjectRequest = s3Presigner.presignGetObject(getObjectPresignRequest);
        return presignedGetObjectRequest.url().toString();
    }

    /**
     * 上传文件.
     *
     * @param key  S3 对象键
     * @param file 要上传的文件
     */
    public void uploadFile(String key, File file) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        s3Client.putObject(putObjectRequest, RequestBody.fromFile(file));
    }

    /**
     * 上传字节数组.
     *
     * @param key     S3 对象键
     * @param content 字节数组
     */
    public void uploadBytes(String key, byte[] content) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(content));
    }

    /**
     * 下载到本地文件.
     *
     * @param key  S3 对象键
     * @param path 下载到的本地路径
     */
    public void downloadFile(String key, Path path) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        s3Client.getObject(getObjectRequest, path);
    }

    /**
     * 删除文件.
     *
     * @param key S3 对象键
     */
    public void deleteFile(String key) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        s3Client.deleteObject(deleteObjectRequest);
    }

    /**
     * 批量删除文件.
     *
     * @param keys S3 对象键列表
     */
    public void deleteObjects(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }

        List<ObjectIdentifier> objectsToDelete = keys.stream()
                .map(key -> ObjectIdentifier.builder().key(key).build())
                .collect(Collectors.toList());

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

    /**
     * 获取文件输入流.
     *
     * @param key S3 对象键
     * @return 输入流
     */
    public InputStream getInputStream(String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        return s3Client.getObject(getObjectRequest);
    }

    /**
     * 判断对象是否存在.
     *
     * @param key S3 对象键
     * @return 是否存在
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
     * 批量上传目录下的文件.
     *
     * @param keyPrefix S3 键前缀
     * @param directory 本地目录
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
     * 递归删除 S3 目录（按前缀删除）.
     *
     * @param prefix S3 键前缀
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

    /**
     * 异步上传文件到 S3.
     * 使用 Async 注解在 Virtual Thread 上执行。
     *
     * @param key  S3 对象键
     * @param file 要上传的文件
     * @return 异步操作结果
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
     * 异步从 S3 下载文件.
     * 使用 Async 注解在 Virtual Thread 上执行。
     *
     * @param key  S3 对象键
     * @param path 下载到的本地路径
     * @return 异步操作结果
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
     * 异步从 S3 删除文件.
     * 使用 Async 注解在 Virtual Thread 上执行。
     *
     * @param key S3 对象键
     * @return 异步操作结果
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
     * 异步批量删除 S3 目录.
     * 使用 Async 注解在 Virtual Thread 上执行。
     *
     * @param prefix S3 对象键前缀
     * @return 异步操作结果
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

    /**
     * 初始化分片上传.
     *
     * @param key S3 对象键
     * @return uploadId
     */
    public String createMultipartUpload(String key) {
        CreateMultipartUploadRequest createMultipartUploadRequest = CreateMultipartUploadRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        return s3Client.createMultipartUpload(createMultipartUploadRequest).uploadId();
    }

    /**
     * 复制分片（服务端复制）.
     *
     * @param sourceKey      源文件的 Key
     * @param destinationKey 目标文件的 Key
     * @param uploadId       分片上传 ID
     * @param partNumber     分片号
     * @return CompletedPart
     */
    public CompletedPart uploadPartCopy(String sourceKey, String destinationKey, String uploadId, int partNumber) {
        UploadPartCopyRequest uploadPartCopyRequest = UploadPartCopyRequest.builder()
                .sourceBucket(bucketName)
                .sourceKey(sourceKey)
                .destinationBucket(bucketName)
                .destinationKey(destinationKey)
                .uploadId(uploadId)
                .partNumber(partNumber)
                .build();

        UploadPartCopyResponse response = s3Client.uploadPartCopy(uploadPartCopyRequest);
        return CompletedPart.builder()
                .partNumber(partNumber)
                .eTag(response.copyPartResult().eTag())
                .build();
    }

    /**
     * 完成分片上传.
     *
     * @param key      S3 对象键
     * @param uploadId 分片上传 ID
     * @param parts    分片列表
     */
    public void completeMultipartUpload(String key, String uploadId, List<CompletedPart> parts) {
        CompletedMultipartUpload completedMultipartUpload = CompletedMultipartUpload.builder()
                .parts(parts)
                .build();

        CompleteMultipartUploadRequest completeMultipartUploadRequest = CompleteMultipartUploadRequest.builder()
                .bucket(bucketName)
                .key(key)
                .uploadId(uploadId)
                .multipartUpload(completedMultipartUpload)
                .build();

        s3Client.completeMultipartUpload(completeMultipartUploadRequest);
    }

    /**
     * 中止分片上传.
     *
     * @param key      S3 对象键
     * @param uploadId 分片上传 ID
     */
    public void abortMultipartUpload(String key, String uploadId) {
        AbortMultipartUploadRequest abortMultipartUploadRequest = AbortMultipartUploadRequest.builder()
                .bucket(bucketName)
                .key(key)
                .uploadId(uploadId)
                .build();
        s3Client.abortMultipartUpload(abortMultipartUploadRequest);
    }
}
