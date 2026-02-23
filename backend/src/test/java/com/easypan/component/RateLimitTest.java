package com.easypan.component;

import com.easypan.annotation.RateLimit;
import com.easypan.aspect.RateLimitAspect;
import com.easypan.entity.enums.ResponseCodeEnum;
import com.easypan.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitTest {

    private static class TestController {
        @RateLimit(key = "testKey", time = 60, count = 5)
        public void limitedEndpoint() {
        }
    }

    @InjectMocks
    private RateLimitAspect rateLimitAspect;

    @Mock
    private org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature signature;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() throws Throwable {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        when(request.getRequestURI()).thenReturn("/api/file/loadDataList");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        Method method = TestController.class.getDeclaredMethod("limitedEndpoint");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("Test Rate Limit within limit")
    void testInterceptorWithinLimit() throws Throwable {
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString(), anyLong())).thenReturn(1L);
        when(joinPoint.proceed()).thenReturn(null);

        assertDoesNotThrow(() -> rateLimitAspect.interceptor(joinPoint));

        verify(valueOperations, times(1)).increment(anyString(), anyLong());
        verify(stringRedisTemplate, times(1)).expire(anyString(), anyLong(), org.mockito.ArgumentMatchers.any(TimeUnit.class));
        verify(joinPoint, times(1)).proceed();
    }

    @Test
    @DisplayName("Test Rate Limit exceeded")
    void testInterceptorExceeded() throws Throwable {
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString(), anyLong())).thenReturn(6L);

        BusinessException exception = assertThrows(BusinessException.class, () -> rateLimitAspect.interceptor(joinPoint));

        assertEquals(ResponseCodeEnum.CODE_600.getCode(), exception.getCode());
        verify(joinPoint, never()).proceed();
    }
}
