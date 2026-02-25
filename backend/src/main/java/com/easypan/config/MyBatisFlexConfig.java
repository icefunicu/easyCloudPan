package com.easypan.config;

import com.mybatisflex.core.audit.AuditManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * MyBatis-Flex 鍏ㄥ眬閰嶇疆.
 * 寮€鍚?SQL 瀹¤鍔熻兘锛屽苟杈撳嚭姣忔潯 SQL 鐨勬墽琛岃€楁椂鍜屽畬鏁磋鍙?
 */
@Configuration
public class MyBatisFlexConfig {

    private static final Logger logger = LoggerFactory.getLogger("com.easypan.sql");

    /**
     * 鍒濆鍖?MyBatis-Flex SQL 瀹¤鏀堕泦鍣?
     */
    @PostConstruct
    public void init() {
        AuditManager.setAuditEnable(true);

        AuditManager.setMessageCollector(auditMessage -> {
            long elapsedTime = auditMessage.getElapsedTime();
            String sql = auditMessage.getFullSql();

            if (elapsedTime > 1000) {
                logger.warn("[Slow SQL] cost={}ms, sql={}", elapsedTime, sql);
            } else if (logger.isDebugEnabled()) {
                logger.debug("[SQL] cost={}ms, sql={}", elapsedTime, sql);
            }
        });
    }
}