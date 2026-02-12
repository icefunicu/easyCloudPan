package com.easypan.component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 租户拦截器
 * 负责从请求中提取租户标识，并设置到上下文
 */
@Component
public class TenantInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(TenantInterceptor.class);
    private static final String TENANT_HEADER = "X-Tenant-Id";
    private static final String DOMAIN_SUFFIX = ".easypan.com";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 从请求头获取租户 ID
        String tenantId = request.getHeader(TENANT_HEADER);
        logger.debug("Tenant ID from header: {}", tenantId);

        // 2. 如果没有指定，尝试从域名解析
        if (tenantId == null || tenantId.isEmpty()) {
            String host = request.getServerName();
            tenantId = parseTenantFromHost(host);
            logger.debug("Tenant ID parsed from host ({}): {}", host, tenantId);
        }

        // 3. 设置租户上下文
        TenantContextHolder.setTenantId(tenantId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 清理租户上下文，防止内存泄漏和上下文污染
        TenantContextHolder.clear();
    }

    private String parseTenantFromHost(String host) {
        // 从子域名解析租户: tenant1.easypan.com -> tenant1
        if (host != null && host.endsWith(DOMAIN_SUFFIX)) {
            String prefix = host.substring(0, host.length() - DOMAIN_SUFFIX.length());
            // 排除 www
            if (!"www".equals(prefix) && !prefix.contains(".")) {
                return prefix;
            }
        }
        return "default";
    }
}
