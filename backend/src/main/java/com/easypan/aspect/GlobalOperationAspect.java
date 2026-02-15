package com.easypan.aspect;

import com.easypan.entity.dto.SessionWebUserDto;
import com.easypan.entity.enums.ResponseCodeEnum;
import com.easypan.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * 全局操作切面，用于处理登录校验和参数校验.
 */
@Aspect
@Component
public class GlobalOperationAspect {

    private static final Logger logger = LoggerFactory.getLogger(GlobalOperationAspect.class);

    @Pointcut("@annotation(com.easypan.annotation.GlobalInterceptor)")
    private void requestInterceptor() {
    }

    /**
     * 拦截带有 GlobalInterceptor 注解的方法，执行登录校验和参数校验.
     *
     * @param point 切点
     * @return 方法执行结果
     * @throws Throwable 异常
     */
    @Around("requestInterceptor()")
    public Object interceptorDo(ProceedingJoinPoint point) throws Throwable {
        final long startTime = System.currentTimeMillis();
        Object target = point.getTarget();
        Object[] arguments = point.getArgs();
        String methodName = point.getSignature().getName();
        Class<?>[] parameterTypes = ((MethodSignature) point.getSignature()).getMethod().getParameterTypes();
        Method method = target.getClass().getMethod(methodName, parameterTypes);
        com.easypan.annotation.GlobalInterceptor interceptor = method
                .getAnnotation(com.easypan.annotation.GlobalInterceptor.class);

        if (interceptor == null) {
            return point.proceed();
        }

        // 校验登录
        if (interceptor.checkLogin() || interceptor.checkAdmin()) {
            checkLogin(interceptor.checkAdmin());
        }

        // 校验参数
        if (interceptor.checkParams()) {
            validateParams(method, arguments);
        }

        Object result = point.proceed();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        if (duration > 1000) {
            logger.warn("Slow API execution: {}.{} took {}ms", target.getClass().getSimpleName(), methodName, duration);
        } else if (logger.isDebugEnabled()) {
            logger.debug("API execution: {}.{} took {}ms", target.getClass().getSimpleName(), methodName, duration);
        }

        return result;
    }

    private void checkLogin(boolean checkAdmin) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                .getRequest();
        HttpSession session = request.getSession();
        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto) session
                .getAttribute(com.easypan.entity.constants.Constants.SESSION_KEY);
        if (sessionWebUserDto == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }
        if (checkAdmin && !sessionWebUserDto.getAdmin()) {
            throw new BusinessException(ResponseCodeEnum.CODE_404);
        }
    }

    private void validateParams(Method method, Object[] arguments) {
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Object value = arguments[i];
            com.easypan.annotation.VerifyParam verifyParam = parameter
                    .getAnnotation(com.easypan.annotation.VerifyParam.class);
            if (verifyParam == null) {
                continue;
            }
            if (value == null) {
                if (verifyParam.required()) {
                    throw new BusinessException(ResponseCodeEnum.CODE_600);
                }
                continue;
            }
            if (value instanceof String) {
                checkString((String) value, verifyParam);
            } else if (value instanceof Integer) {
                checkInteger((Integer) value, verifyParam);
            } else if (value instanceof Long) {
                checkLong((Long) value, verifyParam);
            }
        }
    }

    private void checkString(String value, com.easypan.annotation.VerifyParam verifyParam) {
        if (com.easypan.utils.StringTools.isEmpty(value)) {
            if (verifyParam.required()) {
                throw new BusinessException(ResponseCodeEnum.CODE_600);
            }
            return;
        }
        int min = verifyParam.min();
        int max = verifyParam.max();
        // -1 means "no constraint"
        if (min >= 0 && value.length() < min) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (max >= 0 && value.length() > max) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (verifyParam.regex() != null && verifyParam.regex() != com.easypan.entity.enums.VerifyRegexEnum.NO) {
            if (!value.matches(verifyParam.regex().getRegex())) {
                throw new BusinessException(ResponseCodeEnum.CODE_600);
            }
        }
    }

    private void checkInteger(Integer value, com.easypan.annotation.VerifyParam verifyParam) {
        if (value == null) {
            if (verifyParam.required()) {
                throw new BusinessException(ResponseCodeEnum.CODE_600);
            }
            return;
        }
        int min = verifyParam.min();
        int max = verifyParam.max();
        if (min >= 0 && value < min) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (max >= 0 && value > max) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
    }

    private void checkLong(Long value, com.easypan.annotation.VerifyParam verifyParam) {
        if (value == null) {
            if (verifyParam.required()) {
                throw new BusinessException(ResponseCodeEnum.CODE_600);
            }
            return;
        }
        int min = verifyParam.min();
        int max = verifyParam.max();
        if (min >= 0 && value < min) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (max >= 0 && value > max) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
    }
}
