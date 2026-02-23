package com.easypan.config;

import com.mybatisflex.core.audit.AuditManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * MyBatis-Flex 全局配置.
 * 开启 SQL 审计功能，并以人性化格式输出每条 SQL 的执行耗时和完整语句.
 */
@Configuration
public class MyBatisFlexConfig {

    private static final Logger logger = LoggerFactory.getLogger("com.easypan.sql");

    /**
     * 初始化 MyBatis-Flex SQL 审计收集器.
     * 在应用启动时自动开启 SQL 审计，将每条 SQL 的耗时与完整语句以 Emoji 格式输出到日志.
     */
    @PostConstruct
    public void init() {
        // 开启 SQL 审计功能
        AuditManager.setAuditEnable(true);

        // 设置 SQL 审计收集器，实现人性化日志输出
        AuditManager.setMessageCollector(auditMessage -> {
            long elapsedTime = auditMessage.getElapsedTime();
            String sql = auditMessage.getFullSql();

            // 格式化输出
            if (elapsedTime > 1000) {
                logger.warn("🐢 [Slow SQL] 耗时: {}ms | SQL: {}", elapsedTime, sql);
            } else {
                logger.info("⚡ [DB] 耗时: {}ms | SQL: {}", elapsedTime, sql);
            }
        });
    }
}
