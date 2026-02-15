package com.easypan.unit.aspect;

import com.easypan.annotation.VerifyParam;
import com.easypan.aspect.GlobalOperationAspect;
import com.easypan.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GlobalOperationAspect parameter validation")
class GlobalOperationAspectParamValidationTest {

    private final GlobalOperationAspect aspect = new GlobalOperationAspect();

    private static class DummyApi {
        @SuppressWarnings("unused")
        void login(
                @VerifyParam(required = true) String email,
                @VerifyParam(required = true) String password,
                @VerifyParam(required = true) String checkCode
        ) {
        }

        @SuppressWarnings("unused")
        void withMinMax(@VerifyParam(required = true, min = 2, max = 4) String value) {
        }

        @SuppressWarnings("unused")
        void withRequiredInteger(@VerifyParam(required = true) Integer value) {
        }

        @SuppressWarnings("unused")
        void withRangeInteger(@VerifyParam(required = true, min = 1, max = 3) Integer value) {
        }
    }

    private void validateParams(Method method, Object... args) {
        try {
            Method validateParams = GlobalOperationAspect.class.getDeclaredMethod("validateParams", Method.class, Object[].class);
            validateParams.setAccessible(true);
            validateParams.invoke(aspect, new Object[]{method, args});
        } catch (InvocationTargetException e) {
            Throwable target = e.getTargetException();
            if (target instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException(target);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("required String without min/max should accept non-empty values")
    void requiredStringWithoutMinMaxShouldAcceptNonEmptyValues() throws Exception {
        Method m = DummyApi.class.getDeclaredMethod("login", String.class, String.class, String.class);
        assertDoesNotThrow(() -> validateParams(m, "a@b.com", "pwd", "abcd"));
    }

    @Test
    @DisplayName("required String should reject empty value")
    void requiredStringShouldRejectEmptyValue() throws Exception {
        Method m = DummyApi.class.getDeclaredMethod("login", String.class, String.class, String.class);
        BusinessException ex = assertThrows(BusinessException.class, () -> validateParams(m, "", "pwd", "abcd"));
        assertEquals(600, ex.getCode());
    }

    @Test
    @DisplayName("required String should reject null value")
    void requiredStringShouldRejectNullValue() throws Exception {
        Method m = DummyApi.class.getDeclaredMethod("login", String.class, String.class, String.class);
        BusinessException ex = assertThrows(BusinessException.class, () -> validateParams(m, null, "pwd", "abcd"));
        assertEquals(600, ex.getCode());
    }

    @Test
    @DisplayName("min/max on String should be enforced only when configured")
    void minMaxStringShouldBeEnforced() throws Exception {
        Method m = DummyApi.class.getDeclaredMethod("withMinMax", String.class);
        assertThrows(BusinessException.class, () -> validateParams(m, "a"));
        assertDoesNotThrow(() -> validateParams(m, "ab"));
        assertDoesNotThrow(() -> validateParams(m, "abcd"));
        assertThrows(BusinessException.class, () -> validateParams(m, "abcde"));
    }

    @Test
    @DisplayName("required Integer without min/max should accept any non-null value")
    void requiredIntegerWithoutMinMaxShouldAcceptNonNullValues() throws Exception {
        Method m = DummyApi.class.getDeclaredMethod("withRequiredInteger", Integer.class);
        assertDoesNotThrow(() -> validateParams(m, 0));
        assertDoesNotThrow(() -> validateParams(m, -1));
    }

    @Test
    @DisplayName("min/max on Integer should be enforced only when configured")
    void minMaxIntegerShouldBeEnforced() throws Exception {
        Method m = DummyApi.class.getDeclaredMethod("withRangeInteger", Integer.class);
        assertThrows(BusinessException.class, () -> validateParams(m, 0));
        assertDoesNotThrow(() -> validateParams(m, 1));
        assertDoesNotThrow(() -> validateParams(m, 3));
        assertThrows(BusinessException.class, () -> validateParams(m, 4));
    }
}

