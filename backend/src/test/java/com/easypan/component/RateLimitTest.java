package com.easypan.component;

import com.easypan.annotation.RateLimit;
import com.easypan.aspect.RateLimitAspect;
import com.easypan.entity.enums.ResponseCodeEnum;
import com.easypan.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitTest {

    @InjectMocks
    private RateLimitAspect rateLimitAspect;

    @Mock
    private RedisUtils<Integer> redisUtils;

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private MethodSignature signature;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpSession session;

    @Mock
    private RateLimit rateLimit;

    @BeforeEach
    void setUp() {
        // Mock RequestContextHolder
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        when(request.getSession()).thenReturn(session);

        // Mock JoinPoint signature
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(this.getClass().getMethods()[0]); // Just any method
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("Test Rate Limit within limit")
    void testDoBefore_WithinLimit() {
        when(rateLimit.key()).thenReturn("testKey");
        when(rateLimit.time()).thenReturn(60);
        when(rateLimit.count()).thenReturn(5);
        when(redisUtils.increment(anyString(), anyLong())).thenReturn(1L);

        assertDoesNotThrow(() -> rateLimitAspect.doBefore(joinPoint, rateLimit));

        verify(redisUtils, times(1)).increment(anyString(), anyLong());
        verify(redisUtils, times(1)).expire(anyString(), anyLong());
    }

    @Test
    @DisplayName("Test Rate Limit exceeded")
    void testDoBefore_Exceeded() {
        when(rateLimit.key()).thenReturn("testKey");
        when(rateLimit.time()).thenReturn(60);
        when(rateLimit.count()).thenReturn(5);
        when(redisUtils.increment(anyString(), anyLong())).thenReturn(6L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> rateLimitAspect.doBefore(joinPoint, rateLimit));

        assertEquals(ResponseCodeEnum.CODE_905.getCode(), exception.getCode());
    }
}
