package com.easypan.component;

import com.easypan.service.JwtBlacklistService;
import com.easypan.service.TokenSecurityAuditService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

/**
 * JWT 认证过滤器，用于解析和验证 JWT Token.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String QUERY_TOKEN_SUNSET = "Tue, 30 Jun 2026 00:00:00 GMT";

    @Resource
    private JwtTokenProvider tokenProvider;

    @Resource
    private JwtBlacklistService jwtBlacklistService;

    @Resource
    private TokenSecurityAuditService tokenSecurityAuditService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            TokenExtractionResult tokenResult = getJwtFromRequest(request);
            String jwt = tokenResult.token();

            if (tokenResult.fromQueryParam()) {
                logger.warn("[TOKEN_DEPRECATION] Query token is deprecated: path={}, ip={}",
                        request.getRequestURI(), getClientIp(request));
                response.setHeader("Deprecation", "true");
                response.setHeader("Sunset", QUERY_TOKEN_SUNSET);
            }

            if (StringUtils.hasText(jwt)) {
                if (jwtBlacklistService.isBlacklisted(jwt)) {
                    logger.warn("[SECURITY_ALERT] Rejected blacklisted JWT from IP: {}",
                            getClientIp(request));
                    tokenSecurityAuditService.reportSuspiciousActivity(
                            "unknown", "BLACKLISTED_TOKEN", "IP: " + getClientIp(request));
                } else if (tokenProvider.validateToken(jwt)) {
                    String userId = tokenProvider.getUserIdFromJWT(jwt);

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userId, null, new ArrayList<>());
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    tokenSecurityAuditService.recordTokenUsage(
                            userId, jwt, getClientIp(request),
                            request.getHeader("User-Agent"));

                    logger.debug("Set Authentication to security context for '{}'", userId);
                } else {
                    logger.warn("[TOKEN_AUDIT] Invalid token used from IP: {}",
                            getClientIp(request));
                }
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    private TokenExtractionResult getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return new TokenExtractionResult(bearerToken.substring(7), false);
        } else if (StringUtils.hasText(bearerToken)) {
            return new TokenExtractionResult(bearerToken, false);
        }

        String tokenParam = request.getParameter("token");
        if (StringUtils.hasText(tokenParam)) {
            return new TokenExtractionResult(tokenParam, true);
        }

        return new TokenExtractionResult(null, false);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private record TokenExtractionResult(String token, boolean fromQueryParam) {
    }
}
