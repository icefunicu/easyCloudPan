package com.easypan.controller;

import com.easypan.entity.enums.ResponseCodeEnum;
import com.easypan.entity.vo.ResponseVO;
import com.easypan.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.nio.file.AccessDeniedException;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 全局异常处理器，统一处理应用中的各类异常，返回友好的错误信息.
 */
@Slf4j
@RestControllerAdvice
public class AGlobalExceptionHandlerController extends ABaseController {

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Value("${dev:false}")
    private boolean isDevelopmentMode;

    @Autowired
    private MessageSource messageSource;

    /**
     * 获取国际化消息，根据当前请求的语言环境返回对应的错误消息.
     *
     * @param code 消息代码
     * @param args 消息参数
     * @return 国际化消息
     */
    private String getMessage(String code, Object[] args) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(code, args, code, locale);
    }

    /**
     * 业务异常处理，处理应用中主动抛出的业务异常，支持国际化错误消息.
     *
     * @param e       业务异常
     * @param request HTTP 请求
     * @return 错误响应
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseVO<Void> handleBusinessException(
            BusinessException e, HttpServletRequest request) {
        String requestUrl = request.getRequestURI();
        String method = request.getMethod();

        Integer code = e.getCode() != null ? e.getCode() : ResponseCodeEnum.CODE_600.getCode();

        String message;
        if (e.getMessage() != null && !e.getMessage().isEmpty()) {
            message = e.getMessage();
        } else {
            String messageKey = "error." + code;
            message = getMessage(messageKey, null);
        }

        // 获取错误解决建议
        String suggestion = null;
        ResponseCodeEnum codeEnum = ResponseCodeEnum.getByCode(code);
        if (codeEnum != null) {
            suggestion = codeEnum.getSuggestion();
        }

        log.warn("⚠️ [业务异常] {} {} - 错误码: {}, 消息: {}",
                method, requestUrl, code, message);

        return ResponseVO.<Void>builder()
                .status(STATUC_ERROR)
                .code(code)
                .info(message)
                .suggestion(suggestion)
                .build();
    }

    /**
     * 参数校验异常处理，处理 Valid 和 Validated 注解触发的参数校验异常.
     *
     * @param e       参数校验异常
     * @param request HTTP 请求
     * @return 错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseVO<Void> handleValidationException(
            MethodArgumentNotValidException e, HttpServletRequest request) {
        String requestUrl = request.getRequestURI();

        // 收集所有字段的校验错误信息
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("❌ [参数校验失败] {} - 详情: {}", requestUrl, message);

        String errorMessage = getMessage("error.600", null) + ": " + message;

        return ResponseVO.<Void>builder()
                .status(STATUC_ERROR)
                .code(ResponseCodeEnum.CODE_600.getCode())
                .info(errorMessage)
                .suggestion(ResponseCodeEnum.CODE_600.getSuggestion())
                .build();
    }

    /**
     * 文件上传大小超限异常处理，处理文件上传时超过配置的最大文件大小限制.
     *
     * @param e       文件上传异常
     * @param request HTTP 请求
     * @return 错误响应
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseVO<Void> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException e, HttpServletRequest request) {

        String requestUrl = request.getRequestURI();
        long maxSize = e.getMaxUploadSize();
        long maxSizeMB = maxSize / (1024 * 1024);

        log.warn("📦 [文件过大] {} - 超出限制: 最大 {}MB",
                requestUrl, maxSizeMB);

        String message = getMessage("error.file.size.exceeded", new Object[] { maxSizeMB });

        return ResponseVO.<Void>builder()
                .status(STATUC_ERROR)
                .code(ResponseCodeEnum.CODE_600.getCode())
                .info(message)
                .suggestion("请压缩文件后重试上传")
                .build();
    }

    /**
     * 404 异常处理，处理请求的资源不存在的情况.
     *
     * @param e       404 异常
     * @param request HTTP 请求
     * @return 错误响应
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseVO<Void> handleNotFoundException(
            NoHandlerFoundException e, HttpServletRequest request) {
        String requestUrl = request.getRequestURI();

        log.warn("🔍 [资源不存在] {} - 未找到对应处理器", requestUrl);

        String message = getMessage("error.not.found", null);

        return ResponseVO.<Void>builder()
                .status(STATUC_ERROR)
                .code(ResponseCodeEnum.CODE_404.getCode())
                .info(message)
                .suggestion(ResponseCodeEnum.CODE_404.getSuggestion())
                .build();
    }

    /**
     * 权限不足异常处理.
     *
     * @param e       权限异常
     * @param request HTTP 请求
     * @return 错误响应
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseVO<Void> handleAccessDeniedException(
            AccessDeniedException e, HttpServletRequest request) {
        String requestUrl = request.getRequestURI();

        log.warn("🔒 [权限不足] {} - 拒绝访问", requestUrl);

        String message = getMessage("error.permission.denied", null);

        return ResponseVO.<Void>builder()
                .status(STATUC_ERROR)
                .code(ResponseCodeEnum.CODE_901.getCode())
                .info(message)
                .suggestion(ResponseCodeEnum.CODE_901.getSuggestion())
                .build();
    }

    /**
     * 参数类型错误异常处理.
     *
     * @param e       参数异常
     * @param request HTTP 请求
     * @return 错误响应
     */
    @ExceptionHandler({ BindException.class, MethodArgumentTypeMismatchException.class })
    public ResponseVO<Void> handleParameterException(
            Exception e, HttpServletRequest request) {
        String requestUrl = request.getRequestURI();

        log.warn("🔧 [参数类型错误] {} - 详情: {}",
                requestUrl, e.getMessage());

        String message = getMessage("error.600", null);

        return ResponseVO.<Void>builder()
                .status(STATUC_ERROR)
                .code(ResponseCodeEnum.CODE_600.getCode())
                .info(message)
                .suggestion(ResponseCodeEnum.CODE_600.getSuggestion())
                .build();
    }

    /**
     * 数据库主键冲突异常处理.
     *
     * @param e       主键冲突异常
     * @param request HTTP 请求
     * @return 错误响应
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseVO<Void> handleDuplicateKeyException(
            DuplicateKeyException e, HttpServletRequest request) {
        String requestUrl = request.getRequestURI();

        log.warn("🔑 [主键冲突] {} - 详情: {}", requestUrl, e.getMessage());

        String message = getMessage("error.601", null);

        return ResponseVO.<Void>builder()
                .status(STATUC_ERROR)
                .code(ResponseCodeEnum.CODE_601.getCode())
                .info(message)
                .suggestion(ResponseCodeEnum.CODE_601.getSuggestion())
                .build();
    }

    /**
     * T20: 数据访问异常处理（连接池耗尽、SQL 超时等）.
     *
     * @param e       数据访问异常
     * @param request HTTP 请求
     * @return 错误响应
     */
    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    public ResponseVO<Void> handleDataAccessException(
            org.springframework.dao.DataAccessException e, HttpServletRequest request) {
        String requestUrl = request.getRequestURI();

        log.error("🗄️ [数据访问异常] {} - 详情: {}", requestUrl, e.getMessage(), e);

        return ResponseVO.<Void>builder()
                .status(STATUC_ERROR)
                .code(ResponseCodeEnum.CODE_500.getCode())
                .info("数据库服务暂时不可用，请稍后再试")
                .suggestion("如果问题持续，请联系管理员")
                .build();
    }

    /**
     * 系统异常处理（兜底），处理所有未被特定处理器捕获的异常.
     * 生产环境下会脱敏，不暴露详细错误信息。
     *
     * @param e       异常
     * @param request HTTP 请求
     * @return 错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseVO<Object> handleException(Exception e, HttpServletRequest request) {
        String requestUrl = request.getRequestURI();
        String method = request.getMethod();

        log.error("💥 [系统异常] {} {} - 错误: {}", method, requestUrl, e.getMessage(), e);

        // 生产环境脱敏，不暴露堆栈信息和详细错误
        String message;
        boolean isProduction = "prod".equals(activeProfile) || !isDevelopmentMode;

        if (isProduction) {
            // 生产环境：只返回通用错误消息
            message = getMessage("error.system", null);
        } else {
            // 开发/测试环境：返回详细错误信息，便于调试
            message = getMessage("error.system", null) + ": " + e.getMessage();
        }

        ResponseVO<Object> response = ResponseVO.builder()
                .status(STATUC_ERROR)
                .code(ResponseCodeEnum.CODE_500.getCode())
                .info(message)
                .suggestion(ResponseCodeEnum.CODE_500.getSuggestion())
                .build();

        // 开发/测试环境：在响应数据中附加详细错误信息和堆栈跟踪
        if (!isProduction) {
            response.setData(buildDetailedErrorInfo(e, requestUrl, method));
        }

        return response;
    }

    /**
     * 构建详细错误信息（仅用于开发/测试环境）.
     *
     * @param e          异常
     * @param requestUrl 请求 URL
     * @param method     请求方法
     * @return 详细错误信息
     */
    private DetailedErrorInfo buildDetailedErrorInfo(
            Exception e, String requestUrl, String method) {
        DetailedErrorInfo errorInfo = new DetailedErrorInfo();
        errorInfo.exceptionType = e.getClass().getName();
        errorInfo.message = e.getMessage();
        errorInfo.requestUrl = requestUrl;
        errorInfo.requestMethod = method;
        errorInfo.stackTrace = getStackTraceAsString(e);
        return errorInfo;
    }

    /**
     * 将堆栈跟踪转换为字符串.
     *
     * @param e 异常
     * @return 堆栈跟踪字符串
     */
    private String getStackTraceAsString(Exception e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * 详细错误信息（仅用于开发/测试环境）.
     */
    @lombok.Data
    private static class DetailedErrorInfo {
        private String exceptionType;
        private String message;
        private String requestUrl;
        private String requestMethod;
        private String stackTrace;
    }
}
