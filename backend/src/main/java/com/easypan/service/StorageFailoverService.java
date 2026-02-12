package com.easypan.service;

import com.easypan.entity.enums.StorageTypeEnum;
import com.easypan.strategy.StorageFactory;
import com.easypan.strategy.StorageStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.io.File;
import java.io.InputStream;

@Service("storageFailoverService")
public class StorageFailoverService implements StorageStrategy {

    private static final Logger logger = LoggerFactory.getLogger(StorageFailoverService.class);

    @Resource
    private StorageFactory storageFactory;

    private StorageStrategy getPrimary() {
        return storageFactory.getStorageStrategy();
    }

    private StorageStrategy getBackup() {
        return storageFactory.getStorageStrategy(StorageTypeEnum.LOCAL.getCode());
    }

    @Override
    public void upload(MultipartFile file, String path) {
        try {
            getPrimary().upload(file, path);
        } catch (Exception e) {
            logger.error("Primary storage upload failed, switching to backup. Path: {}", path, e);
            getBackup().upload(file, path);
        }
    }

    @Override
    public void upload(File file, String path) {
        try {
            getPrimary().upload(file, path);
        } catch (Exception e) {
            logger.error("Primary storage upload failed, switching to backup. Path: {}", path, e);
            getBackup().upload(file, path);
        }
    }

    @Override
    public void uploadDirectory(String prefix, File directory) {
        try {
            getPrimary().uploadDirectory(prefix, directory);
        } catch (Exception e) {
            logger.error("Primary storage upload directory failed, switching to backup. Prefix: {}", prefix, e);
            getBackup().uploadDirectory(prefix, directory);
        }
    }

    @Override
    public InputStream download(String path) {
        try {
            return getPrimary().download(path);
        } catch (Exception e) {
            logger.warn("Primary storage download failed, attempting backup. Path: {}", path, e);
            return getBackup().download(path);
        }
    }

    @Override
    public void delete(String path) {
        try {
            getPrimary().delete(path);
        } catch (Exception e) {
            logger.error("Primary storage delete failed. Path: {}", path, e);
        }
        // Always try to delete from backup as well to ensure consistency or cleanup
        try {
            getBackup().delete(path);
        } catch (Exception e) {
             // Ignore if backup delete fails (file might not exist there)
             // But log debug
             logger.debug("Backup storage delete failed (optional). Path: {}", path, e);
        }
    }

    @Override
    public void deleteDirectory(String path) {
        try {
            getPrimary().deleteDirectory(path);
        } catch (Exception e) {
            logger.error("Primary storage delete directory failed. Path: {}", path, e);
        }
        try {
            getBackup().deleteDirectory(path);
        } catch (Exception e) {
            logger.debug("Backup storage delete directory failed (optional). Path: {}", path, e);
        }
    }

    @Override
    public String getUrl(String path) {
        try {
            return getPrimary().getUrl(path);
        } catch (Exception e) {
             logger.warn("Primary storage getUrl failed, attempting backup. Path: {}", path, e);
             return getBackup().getUrl(path);
        }
    }

    @Override
    public void init() {
        // Init happens at component startup usually
        // We can ignore or delegate
    }
}
