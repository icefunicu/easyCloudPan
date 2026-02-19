package com.easypan.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Trace ID 配置类.
 * 为每个请求生成唯一的 Trace ID，用于全链路追踪.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdConfig extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_MDC_KEY = "traceId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isEmpty()) {
            traceId = generateTraceId();
        }

        MDC.put(TRACE_ID_MDC_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID_MDC_KEY);
        }
    }

    /**
     * 生成 Trace ID.
     * 格式：时间戳-随机数
     */
    private String generateTraceId() {
        return System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 获取当前请求的 Trace ID.
     */
    public static String getCurrentTraceId() {
        return MDC.get(TRACE_ID_MDC_KEY);
    }

    /**
     * 设置当前线程的 Trace ID（用于异步任务）.
     */
    public static void setTraceId(String traceId) {
        if (traceId != null) {
            MDC.put(TRACE_ID_MDC_KEY, traceId);
        }
    }

    /**
     * 清除当前线程的 Trace ID.
     */
    public static void clearTraceId() {
        MDC.remove(TRACE_ID_MDC_KEY);
    }
}
