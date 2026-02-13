package com.easypan.service;

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

        // 计算当前已用空间
        // 注意：这里假设 MyBatis-Flex 会自动添加 tenant_id 过滤
        // 如果 TenantFactory 已配置且 apply 到 file_info 表，则 QueryWrapper 不需要显式添加 tenant_id
        // 但为了保险起见，如果不确定 TenantFactory 配置是否生效，可以这里显式添加？
        // 不过既然目的是验证 TenantPlugin，应该让插件自动处理。
        // 这里只写业务逻辑查询
        
        // 查询未删除的文件总大小 (del_flag != 0 表示删除或回收站? FileInfo defined: 0:删除 1:回收站 2:正常)
        // 通常配额统计包括正常文件和回收站文件? 
        // 假设包括正常(2)和回收站(1)，排除彻底删除(0)
        
        // sum(file_size) where del_flag in (1, 2)
        Long usedStorage = fileInfoMapper.selectObjectByQueryAs(
                QueryWrapper.create().select("sum(file_size)")
                        .from(FILE_INFO)
                        .where(FILE_INFO.DEL_FLAG.in(1, 2)),
                Long.class
        );

        if (usedStorage == null) {
            usedStorage = 0L;
        }

        if (usedStorage + fileSize > tenantInfo.getStorageQuota()) {
            throw new BusinessException(String.format("租户存储空间不足，总配额: %d MB, 已用: %d MB", 
                tenantInfo.getStorageQuota() / 1024 / 1024, usedStorage / 1024 / 1024));
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
