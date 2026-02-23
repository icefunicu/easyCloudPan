package com.easypan.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

/**
 * 请求生命周期摘要日志过滤器.
 * 在每个 HTTP 请求完成后，打印一行包含 Method、URI、状态码、耗时、IP、用户的摘要日志.
 * 静态资源和健康检查路径会被自动跳过以减少噪音.
 */
@Component
@WebFilter(urlPatterns = "/*")
@Order(3)
public class RequestLogFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(RequestLogFilter.class);

    /** 慢请求阈值（毫秒）. */
    private static final long SLOW_THRESHOLD_MS = 500;

    /** 需要被忽略的路径前缀集合，避免打印无价值的日志噪音. */
    private static final Set<String> SKIP_PREFIXES = Set.of(
            "/api/actuator",
            "/api/swagger",
            "/api/v3/api-docs",
            "/api/checkCode",
            "/favicon.ico");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest httpReq)
                || !(response instanceof HttpServletResponse httpResp)) {
            chain.doFilter(request, response);
            return;
        }

        String uri = httpReq.getRequestURI();

        // 跳过不需要记录的路径
        for (String prefix : SKIP_PREFIXES) {
            if (uri.startsWith(prefix)) {
                chain.doFilter(request, response);
                return;
            }
        }

        long startTime = System.currentTimeMillis();

        try {
            chain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            String method = httpReq.getMethod();
            int status = httpResp.getStatus();
            String ip = getClientIp(httpReq);
            String userId = extractUserId();
            String traceId = MDC.get("traceId");

            String userPart = (userId != null) ? " | 👤 " + userId : "";
            String tracePart = (traceId != null) ? " | 🔗 " + traceId : "";

            if (duration > SLOW_THRESHOLD_MS) {
                logger.warn("🐌 {} {} [{}] 耗时 {}ms | 🌐 {}{}{}",
                        method, uri, status, duration, ip, userPart, tracePart);
            } else if (status >= 400) {
                logger.warn("🚫 {} {} [{}] 耗时 {}ms | 🌐 {}{}{}",
                        method, uri, status, duration, ip, userPart, tracePart);
            } else {
                logger.info("📥 {} {} [{}] 耗时 {}ms | 🌐 {}{}{}",
                        method, uri, status, duration, ip, userPart, tracePart);
            }
        }
    }

    /**
     * 从 SecurityContext 中尝试获取当前用户 ID.
     *
     * @return 用户ID，未认证则返回 null
     */
    private String extractUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()
                    && !"anonymousUser".equals(auth.getPrincipal())) {
                return String.valueOf(auth.getPrincipal());
            }
        } catch (Exception ignored) {
            // SecurityContext 不可用时静默忽略
        }
        return null;
    }

    /**
     * 获取客户端真实 IP，兼容反向代理.
     *
     * @param request HTTP 请求
     * @return 客户端 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
