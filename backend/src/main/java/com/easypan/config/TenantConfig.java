package com.easypan.config;

import com.easypan.component.TenantContextHolder;
import com.easypan.component.TenantInterceptor;
import com.mybatisflex.core.tenant.TenantFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 租户配置类
 * 1. 注册 Web 拦截器
 * 2. 配置 MyBatis-Flex 租户工厂
 */
@Configuration
public class TenantConfig implements WebMvcConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(TenantConfig.class);

    @Autowired
    private TenantInterceptor tenantInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        logger.info("Registering TenantInterceptor");
        // 注册租户拦截器，拦截所有 API 请求
        registry.addInterceptor(tenantInterceptor)
                .addPathPatterns("/**")
                // 排除静态资源和不需要租户信息的公开接口
                .excludePathPatterns(
                        "/login", 
                        "/qqlogin/**", 
                        "/api/getAvatar/**", 
                        "/api/file/getImage/**",
                        "/static/**", 
                        "/assets/**",
                        "/doc.html", 
                        "/swagger-ui/**", 
                        "/v3/api-docs/**"
                ); 
    }

    /**
     * 配置 MyBatis-Flex 租户工厂
     * 替代设计文档中提到的 TenantLineInnerInterceptor (MyBatis-Plus 概念)
     */
    @Bean
    public TenantFactory tenantFactory() {
        return new TenantFactory() {
            @Override
            public Object[] getTenantIds() {
                String tenantId = TenantContextHolder.getTenantId();
                if (tenantId == null || "default".equals(tenantId)) {
                    // 如果是默认租户，可能需要返回特定ID或空，视具体表设计而定
                    // 这里假设 default 也是一个存储在 tenant_id 列的值
                    return new Object[]{tenantId};
                }
                return new Object[]{tenantId};
            }
        };
    }
}
