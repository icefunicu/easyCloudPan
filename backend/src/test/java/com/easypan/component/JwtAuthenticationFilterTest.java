package com.easypan.component;

import com.easypan.service.JwtBlacklistService;
import com.easypan.service.TokenSecurityAuditService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter 鉴权与兼容行为测试")
class JwtAuthenticationFilterTest {

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private JwtBlacklistService jwtBlacklistService;

    @Mock
    private TokenSecurityAuditService tokenSecurityAuditService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Authorization 头优先生效，不应触发 query token 弃用头")
    void shouldPreferAuthorizationHeaderOverQueryToken() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/file/loadDataList");
        request.addHeader("Authorization", "Bearer header-token");
        request.setParameter("token", "query-token");
        request.addHeader("User-Agent", "JUnit");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtBlacklistService.isBlacklisted("header-token")).thenReturn(false);
        when(tokenProvider.validateToken("header-token")).thenReturn(true);
        when(tokenProvider.getUserIdFromJWT("header-token")).thenReturn("user-1");

        jwtAuthenticationFilter.doFilter(request, response, new MockFilterChain());

        assertNull(response.getHeader("Deprecation"));
        assertNull(response.getHeader("Sunset"));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals("user-1", authentication.getPrincipal());
        verify(tokenSecurityAuditService).recordTokenUsage(eq("user-1"), eq("header-token"), eq("127.0.0.1"), eq("JUnit"));
    }

    @Test
    @DisplayName("query token 兼容：应返回弃用响应头")
    void shouldSetDeprecationHeadersWhenUsingQueryToken() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/file/getFile/file-1");
        request.setParameter("token", "query-token");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtBlacklistService.isBlacklisted("query-token")).thenReturn(false);
        when(tokenProvider.validateToken("query-token")).thenReturn(false);

        jwtAuthenticationFilter.doFilter(request, response, new MockFilterChain());

        assertEquals("true", response.getHeader("Deprecation"));
        assertEquals("Tue, 30 Jun 2026 00:00:00 GMT", response.getHeader("Sunset"));
    }

    @Test
    @DisplayName("黑名单 token 应拒绝认证并上报安全审计")
    void shouldRejectBlacklistedTokenAndReportAudit() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/file/loadDataList");
        request.addHeader("Authorization", "Bearer blacklisted-token");
        request.setRemoteAddr("10.0.0.10");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtBlacklistService.isBlacklisted("blacklisted-token")).thenReturn(true);

        jwtAuthenticationFilter.doFilter(request, response, new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(tokenSecurityAuditService).reportSuspiciousActivity(eq("unknown"), eq("BLACKLISTED_TOKEN"), anyString());
        verify(tokenProvider, never()).validateToken(anyString());
    }
}
