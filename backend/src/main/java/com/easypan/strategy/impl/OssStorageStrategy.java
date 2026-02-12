package com.easypan.strategy.impl;

import com.easypan.component.S3Component;
import com.easypan.entity.config.AppConfig;
import com.easypan.exception.BusinessException;
import com.easypan.strategy.StorageStrategy;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@Service
public class OssStorageStrategy implements StorageStrategy {

    private static final Logger logger = LoggerFactory.getLogger(OssStorageStrategy.class);

    @Resource
    private S3Component s3Component;

    @Resource
    private AppConfig appConfig;

    @Override
    public void upload(MultipartFile file, String path) {
        try {
            // S3Component handles PutObjectRequest with RequestBody.fromBytes or fromFile
            // For MultipartFile, we can use bytes or transfer to a temp file
            s3Component.uploadBytes(path, file.getBytes());
        } catch (IOException e) {
            logger.error("Upload file to OSS failed", e);
            throw new BusinessException("Upload failed");
        }
    }

    @Override
    public void upload(File file, String path) {
        s3Component.uploadFile(path, file);
    }

    @Override
    public void uploadDirectory(String prefix, File directory) {
        s3Component.uploadDirectory(prefix, directory);
    }

    @Override
    public InputStream download(String path) {
        return s3Component.getInputStream(path);
    }

    @Override
    public void delete(String path) {
        s3Component.deleteFile(path);
    }

    @Override
    public void deleteDirectory(String path) {
        s3Component.deleteDirectory(path);
    }

    @Override
    public String getUrl(String path) {
        // In a real scenario, this might return a presigned URL or a CDN URL.
        // For now, consistent with existing logic, we might not use this explicitly
        // if the controller still proxies the stream.
        // But if we want to redirect, we would return the OSS URL.
        return path;
    }

    @Override
    public void init() {
        // S3Component is already initialized by Spring/Config
    }
}
