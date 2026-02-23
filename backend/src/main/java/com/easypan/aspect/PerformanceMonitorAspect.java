package com.easypan.aspect;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 性能监控切面，用于记录关键接口的响应时间.
 * 同时将 Micrometer Timer 指标推送至 Prometheus（T11 增强）.
 */
@Aspect
@Component
@Slf4j
public class PerformanceMonitorAspect {

    private final MeterRegistry meterRegistry;

    @Autowired
    public PerformanceMonitorAspect(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * 定义切点：监控所有 Controller 的公共方法.
     */
    @Pointcut("execution(public * com.easypan.controller..*.*(..))")
    public void controllerMethods() {
    }

    /**
     * 环绕通知：记录方法执行时间并推送 Micrometer 指标.
     */
    @Around("controllerMethods()")
    public Object monitorPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        long startTime = System.currentTimeMillis();
        boolean success = true;

        try {
            return joinPoint.proceed();
        } catch (Throwable throwable) {
            success = false;
            throw throwable;
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            // T11: 推送 Micrometer Timer 指标
            Timer.builder("http.controller.duration")
                    .tag("class", className)
                    .tag("method", methodName)
                    .tag("success", String.valueOf(success))
                    .description("Controller 方法执行耗时")
                    .register(meterRegistry)
                    .record(duration, TimeUnit.MILLISECONDS);

            // 日志记录（保留原有逻辑）
            if (duration > 1000) {
                log.warn("[PERF] {}.{} took {}ms [SLOW] success={}",
                        className, methodName, duration, success);
            } else if (duration > 300) {
                log.info("[PERF] {}.{} took {}ms success={}",
                        className, methodName, duration, success);
            } else if (log.isDebugEnabled()) {
                log.debug("[PERF] {}.{} took {}ms success={}",
                        className, methodName, duration, success);
            }
        }
    }
}
