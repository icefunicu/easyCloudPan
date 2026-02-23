package com.easypan.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 应用启动横幅，在 SpringBoot 启动完成后打印醒目的系统运行状态信息.
 */
@Component
public class StartupBanner implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(StartupBanner.class);

    @Value("${server.port:7090}")
    private String serverPort;

    @Value("${spring.datasource.url:unknown}")
    private String datasourceUrl;

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private String redisPort;

    @Value("${minio.endpoint:unknown}")
    private String minioEndpoint;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Override
    public void run(ApplicationArguments args) {
        String javaVersion = System.getProperty("java.version");
        String dbInfo = extractDbInfo(datasourceUrl);

        String banner = """

                ╔═══════════════════════════════════════════════╗
                ║                                               ║
                ║   🚀 EasyCloudPan 启动成功！                  ║
                ║                                               ║
                ╠═══════════════════════════════════════════════╣
                ║   📍 端口:     %-30s  ║
                ║   🗄️ 数据库:   %-30s  ║
                ║   🔴 Redis:    %-30s  ║
                ║   📦 MinIO:    %-30s  ║
                ║   ☕ Java:     %-30s  ║
                ║   🏷️ Profile:  %-30s  ║
                ╠═══════════════════════════════════════════════╣
                ║   📝 API 文档:  /api/swagger-ui/index.html    ║
                ║   📊 健康检查:  /api/actuator/health          ║
                ║   📈 Prometheus: /api/actuator/prometheus     ║
                ╚═══════════════════════════════════════════════╝
                """.formatted(
                serverPort,
                dbInfo,
                redisHost + ":" + redisPort,
                minioEndpoint,
                javaVersion,
                activeProfile);

        logger.info(banner);
    }

    /**
     * 从 JDBC URL 中提取数据库连接摘要信息.
     *
     * @param url JDBC 连接字符串
     * @return 精简的数据库描述
     */
    private String extractDbInfo(String url) {
        if (url == null || url.equals("unknown")) {
            return "未配置";
        }
        // jdbc:postgresql://127.0.0.1:5432/easypan -> PostgreSQL 127.0.0.1:5432/easypan
        try {
            String withoutPrefix = url.replace("jdbc:", "");
            int colonIdx = withoutPrefix.indexOf(":");
            String dbType = withoutPrefix.substring(0, colonIdx);
            String hostPart = withoutPrefix.substring(colonIdx + 3); // skip "://"
            return dbType.substring(0, 1).toUpperCase() + dbType.substring(1) + " " + hostPart;
        } catch (Exception e) {
            return url;
        }
    }
}
