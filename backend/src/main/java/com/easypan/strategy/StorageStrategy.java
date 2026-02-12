package com.easypan.strategy;

import java.io.InputStream;
import org.springframework.web.multipart.MultipartFile;

public interface StorageStrategy {
    void upload(MultipartFile file, String path);

    void upload(java.io.File file, String path);

    void uploadDirectory(String prefix, java.io.File directory);

    InputStream download(String path);

    void delete(String path);

    void deleteDirectory(String path);

    String getUrl(String path);

    void init();
}
