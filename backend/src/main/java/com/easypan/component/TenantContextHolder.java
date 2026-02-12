package com.easypan.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 租户上下文持有者
 * 使用 ThreadLocal 存储当前请求的租户 ID
 */
public class TenantContextHolder {

    private static final Logger logger = LoggerFactory.getLogger(TenantContextHolder.class);
    private static final ThreadLocal<String> TENANT_ID = new ThreadLocal<>();
    private static final String DEFAULT_TENANT = "default";

    public static void setTenantId(String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            tenantId = DEFAULT_TENANT;
        }
        logger.debug("Setting tenant context: {}", tenantId);
        TENANT_ID.set(tenantId);
    }

    public static String getTenantId() {
        String tenantId = TENANT_ID.get();
        return tenantId != null ? tenantId : DEFAULT_TENANT;
    }

    public static void clear() {
        logger.debug("Clearing tenant context");
        TENANT_ID.remove();
    }
}
