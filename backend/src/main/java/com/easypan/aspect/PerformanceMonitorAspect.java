package com.easypan.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * 性能监控切面，用于记录关键接口的响应时间.
 */
@Aspect
@Component
@Slf4j
public class PerformanceMonitorAspect {

    /**
     * 定义切点：监控所有 Controller 的公共方法.
     */
    @Pointcut("execution(public * com.easypan.controller..*.*(..))")
    public void controllerMethods() {
    }

    /**
     * 环绕通知：记录方法执行时间.
     */
    @Around("controllerMethods()")
    public Object monitorPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        long startTime = System.currentTimeMillis();
        Object result = null;
        boolean success = true;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable throwable) {
            success = false;
            throw throwable;
        } finally {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // 记录性能日志
            if (duration > 1000) {
                // 超过 1 秒的请求记录为 WARN
                log.warn("[PERF] {}.{} took {}ms [SLOW] success={}",
                        className, methodName, duration, success);
            } else if (duration > 300) {
                // 超过 300ms 的请求记录为 INFO
                log.info("[PERF] {}.{} took {}ms success={}",
                        className, methodName, duration, success);
            } else if (log.isDebugEnabled()) {
                // 正常请求在 DEBUG 级别记录
                log.debug("[PERF] {}.{} took {}ms success={}",
                        className, methodName, duration, success);
            }
        }
    }
}
