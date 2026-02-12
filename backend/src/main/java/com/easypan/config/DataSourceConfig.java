package com.easypan.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * 数据源配置类
 * 针对虚拟线程优化 HikariCP 连接池配置
 */
@Configuration
@Slf4j
public class DataSourceConfig {

    @Value("${virtual-threads.enabled:true}")
    private boolean virtualThreadsEnabled;

    /**
     * 配置 HikariCP 数据源
     * 当启用虚拟线程时，优化连接池参数以支持高并发场景
     *
     * @param properties 数据源属性
     * @return DataSource 数据源对象
     */
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.hikari")
    public DataSource dataSource(DataSourceProperties properties) {
        HikariDataSource dataSource = properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();

        if (virtualThreadsEnabled) {
            log.info("为虚拟线程配置 HikariCP 连接池");
            
            // 增加连接池大小以支持虚拟线程的高并发
            dataSource.setMaximumPoolSize(100);
            dataSource.setMinimumIdle(20);

            // 减少超时时间以防止虚拟线程固定
            dataSource.setConnectionTimeout(5000);
            dataSource.setValidationTimeout(3000);

            // 禁用语句缓存以避免固定问题
            dataSource.addDataSourceProperty("cachePrepStmts", "false");
            
            log.info("HikariCP 配置完成 - 最大连接数: {}, 最小空闲: {}, 连接超时: {}ms, 验证超时: {}ms",
                    dataSource.getMaximumPoolSize(), dataSource.getMinimumIdle(),
                    dataSource.getConnectionTimeout(), dataSource.getValidationTimeout());
        } else {
            log.info("虚拟线程未启用，使用默认 HikariCP 配置");
        }

        return dataSource;
    }
}
