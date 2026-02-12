package com.easypan.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Configuration
@EnableAsync
public class VirtualThreadConfig implements AsyncConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(VirtualThreadConfig.class);

    @Value("${spring.threads.virtual.enabled:true}")
    private boolean virtualThreadsEnabled;

    @Value("${spring.threads.virtual.prefix:vt-async-}")
    private String namePrefix;

    /**
     * 配置 Spring Boot 的嵌入式 Tomcat 使用虚拟线程处理 HTTP 请求
     */
    @Bean
    @ConditionalOnProperty(name = "spring.threads.virtual.enabled", havingValue = "true", matchIfMissing = true)
    public TomcatProtocolHandlerCustomizer<?> protocolHandlerVirtualThreadExecutorCustomizer() {
        return protocolHandler -> {
            logger.info("Configuring Tomcat to use Virtual Threads");
            protocolHandler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        };
    }

    /**
     * 配置 @Async 异步任务默认执行器
     * 支持通过 spring.threads.virtual.enabled 开启或关闭虚拟线程
     */
    @Bean("taskExecutor")
    public AsyncTaskExecutor applicationTaskExecutor() {
        if (virtualThreadsEnabled) {
            logger.info("Configuring Virtual Thread executor for @Async operations with prefix: {}", namePrefix);
            // 使用自定义前缀的虚拟线程工厂
            ThreadFactory factory = Thread.ofVirtual().name(namePrefix, 0).factory();
            return new TaskExecutorAdapter(Executors.newThreadPerTaskExecutor(factory));
        } else {
            logger.warn("Virtual Threads not available or disabled, falling back to platform threads");
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(10);
            executor.setMaxPoolSize(50);
            executor.setQueueCapacity(100);
            executor.setThreadNamePrefix("async-");
            executor.initialize();
            return executor;
        }
    }

    /**
     * 实现 AsyncConfigurer 接口，返回 applicationTaskExecutor Bean
     */
    @Override
    public AsyncTaskExecutor getAsyncExecutor() {
        return applicationTaskExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }

    /**
     * 定义名为 virtualThreadExecutor 的 Bean，用于手动提交任务
     * 总是返回虚拟线程执行器 (如果 JDK 支持)
     */
    @Bean("virtualThreadExecutor")
    public AsyncTaskExecutor virtualThreadExecutor() {
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }
}
