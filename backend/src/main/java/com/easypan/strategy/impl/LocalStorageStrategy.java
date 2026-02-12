package com.easypan.strategy.impl;

import com.easypan.entity.config.AppConfig;
import com.easypan.entity.constants.Constants;
import com.easypan.exception.BusinessException;
import com.easypan.strategy.StorageStrategy;
import jakarta.annotation.Resource;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
public class LocalStorageStrategy implements StorageStrategy {

    private static final Logger logger = LoggerFactory.getLogger(LocalStorageStrategy.class);

    @Resource
    private AppConfig appConfig;

    @Override
    public void upload(MultipartFile file, String path) {
        try {
            String fullPath = getFullPath(path);
            File folder = new File(new File(fullPath).getParent());
            if (!folder.exists()) {
                folder.mkdirs();
            }
            file.transferTo(new File(fullPath));
        } catch (IOException e) {
            logger.error("Upload file to local failed", e);
            throw new BusinessException("Upload failed");
        }
    }

    @Override
    public void upload(File file, String path) {
        try {
            String fullPath = getFullPath(path);
            File targetFile = new File(fullPath);
            File folder = targetFile.getParentFile();
            if (!folder.exists()) {
                folder.mkdirs();
            }
            // Avoid copying if source and target are the same file
            if (file.getAbsolutePath().equals(targetFile.getAbsolutePath())) {
                return;
            }
            FileUtils.copyFile(file, targetFile);
        } catch (IOException e) {
            logger.error("Upload file to local failed", e);
            throw new BusinessException("Upload failed");
        }
    }

    @Override
    public void uploadDirectory(String prefix, File directory) {
        try {
            String fullPath = getFullPath(prefix);
            File targetDir = new File(fullPath);
            // If source and target are the same, do nothing
            if (directory.getAbsolutePath().equals(targetDir.getAbsolutePath())) {
                return;
            }
            FileUtils.copyDirectory(directory, targetDir);
        } catch (IOException e) {
            logger.error("Upload directory to local failed", e);
            throw new BusinessException("Upload directory failed");
        }
    }

    @Override
    public InputStream download(String path) {
        try {
            return new FileInputStream(getFullPath(path));
        } catch (IOException e) {
            logger.error("Download file from local failed", e);
            throw new BusinessException("Download failed");
        }
    }

    @Override
    public void delete(String path) {
        File file = new File(getFullPath(path));
        if (file.exists()) {
            file.delete();
        }
    }

    @Override
    public void deleteDirectory(String path) {
        File file = new File(getFullPath(path));
        if (file.exists()) {
            try {
                FileUtils.deleteDirectory(file);
            } catch (IOException e) {
                logger.error("Delete directory failed: {}", path, e);
                throw new BusinessException("Delete directory failed");
            }
        }
    }

    @Override
    public String getUrl(String path) {
        // Return local path or relative path, controller handles serving
        return path;
    }

    @Override
    public void init() {
        // Check root folder exists
        String root = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE;
        File rootFile = new File(root);
        if (!rootFile.exists()) {
            rootFile.mkdirs();
        }
    }

    private String getFullPath(String path) {
        // Basic path verification to prevent directory traversal could be added here
        return appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + path;
    }
}
