package com.easypan.strategy;

import com.easypan.entity.enums.StorageTypeEnum;
import com.easypan.exception.BusinessException;
import com.easypan.strategy.impl.LocalStorageStrategy;
import com.easypan.strategy.impl.OssStorageStrategy;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StorageFactory implements ApplicationContextAware {

    @Value("${app.storage.type:local}")
    private String storageType;

    private static final Map<String, StorageStrategy> STRATEGY_MAP = new ConcurrentHashMap<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, StorageStrategy> beans = applicationContext.getBeansOfType(StorageStrategy.class);
        beans.values().forEach(strategy -> {
            if (strategy instanceof LocalStorageStrategy) {
                STRATEGY_MAP.put(StorageTypeEnum.LOCAL.getCode(), strategy);
            } else if (strategy instanceof OssStorageStrategy) {
                STRATEGY_MAP.put(StorageTypeEnum.OSS.getCode(), strategy);
                STRATEGY_MAP.put(StorageTypeEnum.MINIO.getCode(), strategy); // MinIO shares logic with OSS usually
            }
        });
    }

    public StorageStrategy getStorageStrategy() {
        StorageStrategy strategy = STRATEGY_MAP.get(storageType);
        if (strategy == null) {
            // Default to local if not found or configured
            strategy = STRATEGY_MAP.get(StorageTypeEnum.LOCAL.getCode());
        }
        if (strategy == null) {
            throw new BusinessException("Storage configuration error: No storage strategy found.");
        }
        return strategy;
    }

    public StorageStrategy getStorageStrategy(String type) {
        return STRATEGY_MAP.get(type);
    }
}
