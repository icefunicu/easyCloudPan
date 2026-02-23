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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 存储故障转移服务，实现主备存储切换.
 * T19: 增加简易熔断机制 — 连续失败 N 次后直走 Backup，避免无效重试.
 */
@Service("storageFailoverService")
public class StorageFailoverService implements StorageStrategy {

    private static final Logger logger = LoggerFactory.getLogger(StorageFailoverService.class);

    @Resource
    private StorageFactory storageFactory;

    /**
     * T19: 简易熔断 — 连续失败阈值 & 计数器.
     */
    private static final int CIRCUIT_BREAKER_THRESHOLD = 3;
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);

    private StorageStrategy getPrimary() {
        return storageFactory.getStorageStrategy();
    }

    private StorageStrategy getBackup() {
        return storageFactory.getStorageStrategy(StorageTypeEnum.LOCAL.getCode());
    }

    /**
     * T19: 熔断检查 — 连续失败超过阈值则直接走 Backup.
     */
    private boolean isCircuitOpen() {
        return consecutiveFailures.get() >= CIRCUIT_BREAKER_THRESHOLD;
    }

    private void onPrimarySuccess() {
        consecutiveFailures.set(0);
    }

    private void onPrimaryFailure() {
        int count = consecutiveFailures.incrementAndGet();
        if (count == CIRCUIT_BREAKER_THRESHOLD) {
            logger.error("🔌 存储熔断触发：Primary 连续失败 {} 次，后续请求将直走 Backup", count);
        }
    }

    /**
     * T19: 重置熔断（供健康检查或管理接口调用）.
     */
    public void resetCircuitBreaker() {
        consecutiveFailures.set(0);
        logger.info("🔄 存储熔断已重置");
    }

    @Override
    public void upload(MultipartFile file, String path) {
        if (isCircuitOpen()) {
            logger.warn("熔断开启，直接使用 Backup 上传: {}", path);
            getBackup().upload(file, path);
            return;
        }
        try {
            getPrimary().upload(file, path);
            onPrimarySuccess();
        } catch (Exception e) {
            onPrimaryFailure();
            logger.error("Primary storage upload failed, switching to backup. Path: {}", path, e);
            getBackup().upload(file, path);
        }
    }

    @Override
    public void upload(File file, String path) {
        if (isCircuitOpen()) {
            logger.warn("熔断开启，直接使用 Backup 上传: {}", path);
            getBackup().upload(file, path);
            return;
        }
        try {
            getPrimary().upload(file, path);
            onPrimarySuccess();
        } catch (Exception e) {
            onPrimaryFailure();
            logger.error("Primary storage upload failed, switching to backup. Path: {}", path, e);
            getBackup().upload(file, path);
        }
    }

    @Override
    public void uploadDirectory(String prefix, File directory) {
        if (isCircuitOpen()) {
            logger.warn("熔断开启，直接使用 Backup 上传目录: {}", prefix);
            getBackup().uploadDirectory(prefix, directory);
            return;
        }
        try {
            getPrimary().uploadDirectory(prefix, directory);
            onPrimarySuccess();
        } catch (Exception e) {
            onPrimaryFailure();
            logger.error("Primary storage upload directory failed, switching to backup. Prefix: {}", prefix, e);
            getBackup().uploadDirectory(prefix, directory);
        }
    }

    @Override
    public InputStream download(String path) {
        if (isCircuitOpen()) {
            logger.warn("熔断开启，直接使用 Backup 下载: {}", path);
            return getBackup().download(path);
        }
        try {
            InputStream result = getPrimary().download(path);
            onPrimarySuccess();
            return result;
        } catch (Exception e) {
            onPrimaryFailure();
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
        try {
            getBackup().delete(path);
        } catch (Exception e) {
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
        if (isCircuitOpen()) {
            logger.warn("熔断开启，直接使用 Backup getUrl: {}", path);
            return getBackup().getUrl(path);
        }
        try {
            String url = getPrimary().getUrl(path);
            onPrimarySuccess();
            return url;
        } catch (Exception e) {
            onPrimaryFailure();
            logger.warn("Primary storage getUrl failed, attempting backup. Path: {}", path, e);
            return getBackup().getUrl(path);
        }
    }

    @Override
    public void init() {
        // 组件启动阶段已完成初始化
    }
}
