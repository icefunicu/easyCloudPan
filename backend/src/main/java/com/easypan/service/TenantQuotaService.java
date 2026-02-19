package com.easypan.service;

import com.easypan.component.RedisComponent;
import com.easypan.component.TenantContextHolder;
import com.easypan.entity.po.TenantInfo;
import com.easypan.exception.BusinessException;
import com.easypan.mappers.FileInfoMapper;
import com.easypan.mappers.TenantInfoMapper;
import com.easypan.mappers.UserInfoMapper;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static com.easypan.entity.po.table.FileInfoTableDef.FILE_INFO;

/**
 * 租户配额管理服务.
 */
@Service
public class TenantQuotaService {

    private static final Logger logger = LoggerFactory.getLogger(TenantQuotaService.class);

    @Resource
    private TenantInfoMapper tenantInfoMapper;

    @Resource
    private FileInfoMapper fileInfoMapper;

    @Resource
    private UserInfoMapper userInfoMapper;

    @Resource
    private RedisComponent redisComponent;

    /**
     * 检查存储配额.
     *
     * @param fileSize 待上传文件大小
     */
    public void checkStorageQuota(Long fileSize) {
        String tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null || "default".equals(tenantId)) {
            // 默认租户可能有很大配额，但还是要检查
        }

        TenantInfo tenantInfo = tenantInfoMapper.selectOneById(tenantId);
        if (tenantInfo == null) {
            // 租户不存在，可能是未初始化的默认租户
            logger.warn("Tenant info not found for tenantId: {}", tenantId);
            return;
        }

        if (tenantInfo.getStatus() != null && tenantInfo.getStatus() == 0) {
            throw new BusinessException("租户已被禁用");
        }

        // 优先从缓存获取已用存储
        Long usedStorage = redisComponent.getTenantUsedStorage(tenantId);
        if (usedStorage == null) {
            // 缓存未命中，查询数据库
            usedStorage = fileInfoMapper.selectObjectByQueryAs(
                    QueryWrapper.create().select("sum(file_size)")
                            .from(FILE_INFO)
                            .where(FILE_INFO.DEL_FLAG.in(1, 2)),
                    Long.class
            );
            if (usedStorage == null) {
                usedStorage = 0L;
            }
            // 写入缓存
            redisComponent.saveTenantUsedStorage(tenantId, usedStorage);
        }

        if (usedStorage + fileSize > tenantInfo.getStorageQuota()) {
            throw new BusinessException(String.format("租户存储空间不足，总配额: %d MB, 已用: %d MB", 
                tenantInfo.getStorageQuota() / 1024 / 1024, usedStorage / 1024 / 1024));
        }
    }

    /**
     * 更新租户已用存储（增量）.
     * 上传成功后调用。
     *
     * @param deltaSize 变化大小（正数增加，负数减少）
     */
    public void updateUsedStorage(Long deltaSize) {
        String tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            return;
        }
        // 增量更新缓存
        Long newValue = redisComponent.incrementTenantUsedStorage(tenantId, deltaSize);
        if (newValue == null) {
            // 缓存不存在，删除让下次查询重建
            redisComponent.deleteTenantUsedStorage(tenantId);
        }
    }

    /**
     * 检查用户配额.
     */
    public void checkUserQuota() {
        String tenantId = TenantContextHolder.getTenantId();
        TenantInfo tenantInfo = tenantInfoMapper.selectOneById(tenantId);
        if (tenantInfo == null) {
            return;
        }

        long userCount = userInfoMapper.selectCountByQuery(QueryWrapper.create());
        
        if (userCount >= tenantInfo.getUserQuota()) {
            throw new BusinessException("租户用户数量已达上限");
        }
    }

    /**
     * 获取租户使用情况.
     *
     * @return 租户信息
     */
    public TenantInfo getTenantUsage() {
        String tenantId = TenantContextHolder.getTenantId();
        return tenantInfoMapper.selectOneById(tenantId);
    }
}
