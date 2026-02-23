package com.easypan.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 租户上下文持有者.
 * 使用 ThreadLocal 存储当前请求的租户 ID.
 */
public class TenantContextHolder {

    private static final Logger logger = LoggerFactory.getLogger(TenantContextHolder.class);
    // 使用 InheritableThreadLocal 确保虚拟线程/子线程继承租户上下文
    private static final InheritableThreadLocal<String> TENANT_ID = new InheritableThreadLocal<>();
    private static final String DEFAULT_TENANT = "default";

    /**
     * 设置租户ID.
     *
     * @param tenantId 租户ID
     */
    public static void setTenantId(String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            tenantId = DEFAULT_TENANT;
        }
        logger.debug("Setting tenant context: {}", tenantId);
        TENANT_ID.set(tenantId);
    }

    /**
     * 获取租户ID.
     *
     * @return 租户ID
     */
    public static String getTenantId() {
        String tenantId = TENANT_ID.get();
        return tenantId != null ? tenantId : DEFAULT_TENANT;
    }

    /**
     * 清除租户上下文.
     */
    public static void clear() {
        logger.debug("Clearing tenant context");
        TENANT_ID.remove();
    }
}
