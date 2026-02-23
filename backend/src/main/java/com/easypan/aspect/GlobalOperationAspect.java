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
import org.springframework.web.context.support.WebApplicationContextUtils;

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

    @jakarta.annotation.Resource
    private com.easypan.component.JwtTokenProvider jwtTokenProvider;

    @jakarta.annotation.Resource
    private com.easypan.service.JwtBlacklistService jwtBlacklistService;

    @jakarta.annotation.Resource
    private com.easypan.component.RedisComponent redisComponent;

    @jakarta.annotation.Resource
    private com.easypan.service.UserInfoService userInfoService;

    private void checkLogin(boolean checkAdmin) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                .getRequest();

        SessionWebUserDto sessionWebUserDto = null;
        String bearerToken = request.getHeader("Authorization");
        String token = null;

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            token = bearerToken.substring(7);
        } else {
            token = bearerToken;
        }

        if (token != null && jwtTokenProvider.validateToken(token) && !jwtBlacklistService.isBlacklisted(token)) {
            String userId = jwtTokenProvider.getUserIdFromJWT(token);
            // 这里为了保证无状态且及时响应空间变化，可以根据 userId 从数据库/Redis中拼装出 Dto。
            // 但原本系统里可能并没有直接获取 SessionWebUserDto 的原子方法，为了快速修补并解耦，我们从 DB 新查或缓存存取.
            com.easypan.entity.po.UserInfo userInfo = userInfoService.getUserInfoByUserId(userId);
            if (userInfo != null) {
                // Spring Context aware bean fetcher or AppConfig instance injection
                com.easypan.entity.config.AppConfig appConfig = WebApplicationContextUtils
                        .getRequiredWebApplicationContext(request.getServletContext())
                        .getBean(com.easypan.entity.config.AppConfig.class);

                sessionWebUserDto = new SessionWebUserDto();
                sessionWebUserDto.setUserId(userInfo.getUserId());
                sessionWebUserDto.setNickName(userInfo.getNickName());
                sessionWebUserDto.setAdmin(
                        org.apache.commons.lang3.ArrayUtils.contains(
                                appConfig.getAdminEmails().split(","),
                                userInfo.getEmail()));
                sessionWebUserDto.setAvatar(userInfo.getQqAvatar());
            }
        }

        // 临时降级回 Session 兜底，以防旧前端的纯 cookie 会话报错
        if (sessionWebUserDto == null) {
            HttpSession session = request.getSession();
            sessionWebUserDto = (SessionWebUserDto) session
                    .getAttribute(com.easypan.entity.constants.Constants.SESSION_KEY);
        }

        // 把最终的有效用户态挂载到 Request Attribute，供 Controller 通过 ABaseController 获取
        if (sessionWebUserDto != null) {
            request.setAttribute(com.easypan.entity.constants.Constants.SESSION_KEY, sessionWebUserDto);
        }

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
            java.util.regex.Pattern pattern = verifyParam.regex().getPattern();
            if (pattern != null && !pattern.matcher(value).matches()) {
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
