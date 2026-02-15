package com.easypan.aspect;

import com.easypan.annotation.RateLimit;
import com.easypan.component.RedisUtils;
import com.easypan.entity.dto.SessionWebUserDto;
import com.easypan.entity.enums.ResponseCodeEnum;
import com.easypan.exception.BusinessException;
// import com.easypan.utils.IpUtils; // Will uncomment if found, or replace with request.getRemoteAddr()
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * 速率限制切面，用于限制接口访问频率.
 */
@Aspect
@Component
public class RateLimitAspect {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitAspect.class);

    @Resource
    private RedisUtils<Integer> redisUtils;

    /**
     * 在方法执行前检查速率限制.
     *
     * @param point     切点
     * @param rateLimit 速率限制注解
     */
    @Before("@annotation(rateLimit)")
    public void doBefore(JoinPoint point, RateLimit rateLimit) {
        String key = rateLimit.key();
        int time = rateLimit.time();
        int count = rateLimit.count();

        String uniqueKey = getUniqueKey(point, key);
        String redisKey = "rate_limit:" + uniqueKey;

        try {
            long countInRedis = redisUtils.increment(redisKey, 1);
            if (countInRedis == 1) {
                redisUtils.expire(redisKey, time);
            }
            if (countInRedis > count) {
                logger.warn("Rate limit exceeded for key: {}", redisKey);
                throw new BusinessException(ResponseCodeEnum.CODE_905.getCode(), "访问过于频繁，请稍后再试");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Rate limit check failed", e);
        }
    }

    private String getUniqueKey(JoinPoint point, String key) {
        String userId = "";
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                    .getRequest();
            HttpSession session = request.getSession();
            SessionWebUserDto userDto = (SessionWebUserDto) session
                    .getAttribute(com.easypan.entity.constants.Constants.SESSION_KEY);
            userId = userDto != null ? userDto.getUserId() : request.getRemoteAddr();
        } catch (Exception e) {
            // ignore
        }

        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getName();

        if (com.easypan.utils.StringTools.isEmpty(key)) {
            return methodName + ":" + userId;
        }
        return key + ":" + userId;
    }
}
