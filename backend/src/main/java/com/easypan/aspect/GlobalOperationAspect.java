package com.easypan.aspect;

import com.easypan.annotation.GlobalInterceptor;
import com.easypan.annotation.VerifyParam;
import com.easypan.entity.config.AppConfig;
import com.easypan.entity.constants.Constants;
import com.easypan.entity.dto.SessionWebUserDto;
import com.easypan.entity.enums.ResponseCodeEnum;
import com.easypan.entity.enums.VerifyRegexEnum;
import com.easypan.entity.po.UserInfo;
import com.easypan.entity.query.UserInfoQuery;
import com.easypan.exception.BusinessException;
import com.easypan.service.UserInfoService;
import com.easypan.utils.StringTools;
import com.easypan.utils.VerifyUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

/**
 * 全局操作切面，处理登录校验和参数校验.
 */
@Component("operationAspect")
@Aspect
public class GlobalOperationAspect {

    private static Logger logger = LoggerFactory.getLogger(GlobalOperationAspect.class);
    private static final String TYPE_STRING = "java.lang.String";
    private static final String TYPE_INTEGER = "java.lang.Integer";
    private static final String TYPE_LONG = "java.lang.Long";

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private AppConfig appConfig;

    @Pointcut("@annotation(com.easypan.annotation.GlobalInterceptor)")
    private void requestInterceptor() {
    }

    /**
     * 拦截器前置处理.
     */
    @Before("requestInterceptor()")
    public void interceptorDo(JoinPoint point) throws BusinessException {
        try {
            Object target = point.getTarget();
            Object[] arguments = point.getArgs();
            String methodName = point.getSignature().getName();
            Class<?>[] parameterTypes =
                    ((MethodSignature) point.getSignature()).getMethod().getParameterTypes();
            Method method = target.getClass().getMethod(methodName, parameterTypes);
            GlobalInterceptor interceptor = method.getAnnotation(GlobalInterceptor.class);
            if (null == interceptor) {
                return;
            }
            // 校验登录
            if (interceptor.checkLogin() || interceptor.checkAdmin()) {
                checkLogin(interceptor.checkAdmin());
            }
            // 校验参数
            if (interceptor.checkParams()) {
                validateParams(method, arguments);
            }
        } catch (BusinessException e) {
            logger.error("全局拦截器异常", e);
            throw e;
        } catch (Throwable e) {
            logger.error("全局拦截器异常", e);
            throw new BusinessException(ResponseCodeEnum.CODE_500);
        }
    }

    private void checkLogin(Boolean checkAdmin) {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }
        HttpServletRequest request = attributes.getRequest();
        HttpSession session = request.getSession();
        SessionWebUserDto sessionUser =
                (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        if (sessionUser == null && appConfig.getDev() != null && appConfig.getDev()) {
            List<UserInfo> userInfoList = userInfoService.findListByParam(new UserInfoQuery());
            if (!userInfoList.isEmpty()) {
                UserInfo userInfo = userInfoList.get(0);
                sessionUser = new SessionWebUserDto();
                sessionUser.setUserId(userInfo.getUserId());
                sessionUser.setNickName(userInfo.getNickName());
                sessionUser.setAdmin(true);
                session.setAttribute(Constants.SESSION_KEY, sessionUser);
            }
        }
        if (null == sessionUser) {
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }

        if (checkAdmin && !sessionUser.getAdmin()) {
            throw new BusinessException(ResponseCodeEnum.CODE_404);
        }
    }

    private void validateParams(Method m, Object[] arguments) throws BusinessException {
        Parameter[] parameters = m.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Object value = arguments[i];
            VerifyParam verifyParam = parameter.getAnnotation(VerifyParam.class);
            if (verifyParam == null) {
                continue;
            }
            String typeName = parameter.getParameterizedType().getTypeName();
            if (TYPE_STRING.equals(typeName)
                    || TYPE_LONG.equals(typeName)
                    || TYPE_INTEGER.equals(typeName)) {
                checkValue(parameter.getName(), value, verifyParam);
            } else {
                checkObjValue(parameter, value);
            }
        }
    }

    private void checkObjValue(Parameter parameter, Object value) {
        try {
            String typeName = parameter.getParameterizedType().getTypeName();
            Class<?> classz = Class.forName(typeName);
            Field[] fields = classz.getDeclaredFields();
            for (Field field : fields) {
                VerifyParam fieldVerifyParam = field.getAnnotation(VerifyParam.class);
                if (fieldVerifyParam == null) {
                    continue;
                }
                field.setAccessible(true);
                Object resultValue = field.get(value);
                checkValue(field.getName(), resultValue, fieldVerifyParam);
            }
        } catch (BusinessException e) {
            logger.error("校验参数失败", e);
            throw e;
        } catch (Exception e) {
            logger.error("校验参数失败", e);
            throw new BusinessException(ResponseCodeEnum.CODE_600.getCode(), "参数格式不正确，请检查输入");
        }
    }

    /**
     * 校验参数.
     *
     * @param paramName 参数名
     * @param value 参数值
     * @param verifyParam 校验注解
     * @throws BusinessException 业务异常
     */
    private void checkValue(String paramName, Object value, VerifyParam verifyParam) throws BusinessException {
        Boolean isEmpty = value == null || StringTools.isEmpty(value.toString());
        Integer length = value == null ? 0 : value.toString().length();
        String paramDisplayName = getParamDisplayName(paramName);

        if (isEmpty && verifyParam.required()) {
            String errorMsg = String.format("%s不能为空", paramDisplayName);
            logger.warn("参数校验失败: {}", errorMsg);
            throw new BusinessException(ResponseCodeEnum.CODE_600.getCode(), errorMsg);
        }

        boolean maxCheck = verifyParam.max() != -1 && verifyParam.max() < length;
        boolean minCheck = verifyParam.min() != -1 && verifyParam.min() > length;
        if (!isEmpty && (maxCheck || minCheck)) {
            String errorMsg = String.format("%s长度%d不符合要求(需要%d-%d位)", 
                    paramDisplayName, length, 
                    verifyParam.min() == -1 ? 0 : verifyParam.min(), 
                    verifyParam.max() == -1 ? 999 : verifyParam.max());
            logger.warn("参数校验失败: {}", errorMsg);
            throw new BusinessException(ResponseCodeEnum.CODE_600.getCode(), errorMsg);
        }
        boolean hasRegex = !StringTools.isEmpty(verifyParam.regex().getRegex());
        if (!isEmpty && hasRegex
                && !VerifyUtils.verify(verifyParam.regex(), String.valueOf(value))) {
            String errorMsg = getRegexErrorMessage(paramDisplayName, verifyParam.regex());
            logger.warn("参数校验失败: {} (输入值: {}, 规则: {})", errorMsg, value, verifyParam.regex().name());
            throw new BusinessException(ResponseCodeEnum.CODE_600.getCode(), errorMsg);
        }
    }

    private String getParamDisplayName(String paramName) {
        java.util.Map<String, String> nameMap = new java.util.HashMap<>();
        nameMap.put("email", "邮箱");
        nameMap.put("password", "密码");
        nameMap.put("nickName", "昵称");
        nameMap.put("checkCode", "图片验证码");
        nameMap.put("emailCode", "邮箱验证码");
        nameMap.put("fileName", "文件名");
        nameMap.put("fileId", "文件ID");
        nameMap.put("shareId", "分享ID");
        nameMap.put("shareCode", "提取码");
        return nameMap.getOrDefault(paramName, paramName);
    }

    private String getRegexErrorMessage(String paramDisplayName, VerifyRegexEnum regex) {
        switch (regex) {
            case EMAIL:
                return paramDisplayName + "格式不正确，请输入有效的邮箱地址";
            case PASSWORD:
                return paramDisplayName + "格式不正确，必须包含数字、字母和特殊字符(~!@#$%^&*_)，长度8-32位";
            case PHONE:
                return paramDisplayName + "格式不正确，请输入有效的手机号码";
            case ACCOUNT:
                return paramDisplayName + "只能包含字母、数字和下划线";
            case COMMON:
                return paramDisplayName + "只能包含数字、字母、中文和下划线";
            case NUMBER_LETTER_UNDER_LINE:
                return paramDisplayName + "只能包含数字、字母和下划线";
            default:
                return paramDisplayName + "格式不正确";
        }
    }
}
