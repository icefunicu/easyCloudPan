package com.easypan.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

/**
 * 虚拟线程监控组件.
 */
@Component
public class VirtualThreadMonitor {

    private static final Logger logger = LoggerFactory.getLogger(VirtualThreadMonitor.class);

    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    /**
     * 每分钟打印一次线程统计信息.
     */
    @Scheduled(fixedRate = 60000)
    public void monitorThreads() {
        int threadCount = threadMXBean.getThreadCount();
        int peakThreadCount = threadMXBean.getPeakThreadCount();
        long totalStartedThreadCount = threadMXBean.getTotalStartedThreadCount();

        // 注意：ThreadMXBean 可能无法准确区分虚拟线程和平台线程，但可以看到总线程数的变化
        // Java 21 提供了 jdk.jfr 事件来监控虚拟线程，这里做简单的应用层监控

        logger.info("Thread Monitor - Current: {}, Peak: {}, Total Started: {}",
                threadCount, peakThreadCount, totalStartedThreadCount);

        // 尝试统计名称包含 "virtual" 的线程 (仅限于 toString 或 name 中包含的情况)
        // 主要是为了确认虚拟线程是否被创建和使用
        long virtualThreadCount = Thread.getAllStackTraces().keySet().stream()
                .filter(t -> t.isVirtual())
                .count();

        logger.info("Virtual Thread Estimate (Active): {}", virtualThreadCount);
    }
}
