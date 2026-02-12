package com.easypan.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * 慢查询监控切面
 * 用于监控数据库查询性能，记录执行时间超过阈值的查询
 */
@Aspect
@Component
@Slf4j
public class SlowQueryMonitorAspect {

    /**
     * 慢查询阈值：100ms
     */
    private static final long SLOW_QUERY_THRESHOLD = 100;

    /**
     * 定义切点：监控所有 Mapper 接口的方法
     */
    @Pointcut("execution(* com.easypan.mappers..*.*(..))")
    public void mapperMethods() {
    }

    /**
     * 环绕通知：监控查询执行时间
     */
    @Around("mapperMethods()")
    public Object monitorQuery(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = null;
        
        try {
            result = joinPoint.proceed();
            return result;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            
            // 如果执行时间超过阈值，记录警告日志
            if (duration > SLOW_QUERY_THRESHOLD) {
                String methodSignature = joinPoint.getSignature().toShortString();
                log.warn("慢查询检测: {} 耗时 {}ms", methodSignature, duration);
            }
        }
    }
}
