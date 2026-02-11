package com.easypan.controller;
import com.easypan.entity.enums.ResponseCodeEnum;
import com.easypan.entity.vo.ResponseVO;
import com.easypan.exception.BusinessException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.file.AccessDeniedException;

@Slf4j
@RestControllerAdvice
public class AGlobalExceptionHandlerController extends ABaseController {

    @ExceptionHandler(value = Exception.class)
    Object handleException(Exception e, HttpServletRequest request) {
        String requestUrl = request.getRequestURL().toString();
        String method = request.getMethod();
        
        log.error("[EXCEPTION] {} {} - Error: {}", method, requestUrl, e.getMessage(), e);
        
        ResponseVO<Void> ajaxResponse = new ResponseVO<>();
        
        // 404 - 资源不存在
        if (e instanceof NoHandlerFoundException) {
            ajaxResponse.setCode(ResponseCodeEnum.CODE_404.getCode());
            ajaxResponse.setInfo("请求的资源不存在");
            ajaxResponse.setStatus(STATUC_ERROR);
            log.warn("[EXCEPTION] 404 Not Found: {}", requestUrl);
        } 
        // 业务异常
        else if (e instanceof BusinessException) {
            BusinessException biz = (BusinessException) e;
            ajaxResponse.setCode(biz.getCode() == null ? ResponseCodeEnum.CODE_600.getCode() : biz.getCode());
            ajaxResponse.setInfo(biz.getMessage());
            ajaxResponse.setStatus(STATUC_ERROR);
            log.warn("[EXCEPTION] Business Error [{}]: {}", biz.getCode(), biz.getMessage());
        } 
        // 权限不足
        else if (e instanceof AccessDeniedException) {
            ajaxResponse.setCode(ResponseCodeEnum.CODE_600.getCode());
            ajaxResponse.setInfo("权限不足，无法访问该资源");
            ajaxResponse.setStatus(STATUC_ERROR);
            log.warn("[EXCEPTION] Access Denied: {}", requestUrl);
        }
        // 参数类型错误
        else if (e instanceof BindException || e instanceof MethodArgumentTypeMismatchException) {
            ajaxResponse.setCode(ResponseCodeEnum.CODE_600.getCode());
            ajaxResponse.setInfo("请求参数格式错误，请检查后重试");
            ajaxResponse.setStatus(STATUC_ERROR);
            log.warn("[EXCEPTION] Invalid Parameters: {}", e.getMessage());
        } 
        // 主键冲突/重复提交
        else if (e instanceof DuplicateKeyException) {
            ajaxResponse.setCode(ResponseCodeEnum.CODE_601.getCode());
            ajaxResponse.setInfo("数据已存在，请勿重复提交");
            ajaxResponse.setStatus(STATUC_ERROR);
            log.warn("[EXCEPTION] Duplicate Key: {}", e.getMessage());
        } 
        // 文件上传相关异常
        else if (e.getMessage() != null && e.getMessage().contains("MultipartException")) {
            ajaxResponse.setCode(ResponseCodeEnum.CODE_600.getCode());
            ajaxResponse.setInfo("文件上传失败，请检查文件大小和格式");
            ajaxResponse.setStatus(STATUC_ERROR);
            log.error("[EXCEPTION] File Upload Error: {}", e.getMessage());
        }
        // 会话失效
        else if (e.getMessage() != null && e.getMessage().contains("session")) {
            ajaxResponse.setCode(ResponseCodeEnum.CODE_901.getCode());
            ajaxResponse.setInfo("登录已过期，请重新登录");
            ajaxResponse.setStatus(STATUC_ERROR);
            log.warn("[EXCEPTION] Session Expired: {}", requestUrl);
        }
        // 其他服务器错误
        else {
            ajaxResponse.setCode(ResponseCodeEnum.CODE_500.getCode());
            ajaxResponse.setInfo("服务器内部错误，请稍后重试");
            ajaxResponse.setStatus(STATUC_ERROR);
            log.error("[EXCEPTION] Internal Server Error: {}", e.getMessage(), e);
        }
        
        return ajaxResponse;
    }
}
