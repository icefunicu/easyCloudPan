package com.easypan.aspect;

import com.easypan.entity.constants.Constants;
import com.easypan.entity.dto.SessionShareDto;
import com.easypan.entity.dto.SessionWebUserDto;
import com.easypan.entity.po.FileInfo;
import com.easypan.entity.query.FileInfoQuery;
import com.easypan.exception.BusinessException;
import com.easypan.service.FileInfoService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

@Aspect
@Component
@Slf4j
public class FileAccessControlAspect {

    @Resource
    private FileInfoService fileInfoService;

    @Around("@annotation(com.easypan.annotation.FileAccessCheck)")
    public Object checkFileAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String fileId = extractFileId(joinPoint.getArgs(), signature.getParameterNames());
        String userIdParam = extractUserId(joinPoint.getArgs(), signature.getParameterNames());
        
        if (fileId == null) {
            return joinPoint.proceed();
        }
        
        if (userIdParam != null) {
            FileInfo fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(fileId, userIdParam);
            if (fileInfo == null) {
                log.warn("[FILE_ACCESS] File not found or access denied: fileId={}, userId={}", fileId, userIdParam);
                return null;
            }
            return joinPoint.proceed();
        }
        
        SessionWebUserDto currentUser = getCurrentUser();
        if (currentUser == null) {
            SessionShareDto shareSession = getShareSession(fileId);
            if (shareSession != null) {
                return joinPoint.proceed();
            }
            throw new BusinessException("请先登录");
        }

        FileInfo fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(fileId, currentUser.getUserId());
        
        if (fileInfo == null) {
            if (Boolean.TRUE.equals(currentUser.getAdmin())) {
                 FileInfoQuery query = new FileInfoQuery();
                 query.setFileId(fileId);
                 List<FileInfo> list = fileInfoService.findListByParam(query);
                 if (list != null && !list.isEmpty()) {
                      return joinPoint.proceed();
                 }
            }
            throw new BusinessException("文件不存在或无权访问");
        }

        return joinPoint.proceed();
    }

    private SessionWebUserDto getCurrentUser() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        HttpSession session = request.getSession();
        return (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
    }
    
    private SessionShareDto getShareSession(String shareId) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        HttpSession session = request.getSession();
        return (SessionShareDto) session.getAttribute(Constants.SESSION_SHARE_KEY + shareId);
    }

    private String extractFileId(Object[] args, String[] parameterNames) {
        if (parameterNames == null || args == null) {
            return null;
        }
        for (int i = 0; i < parameterNames.length; i++) {
            if ("fileId".equals(parameterNames[i]) && args[i] instanceof String) {
                return (String) args[i];
            }
        }
        return null;
    }
    
    private String extractUserId(Object[] args, String[] parameterNames) {
        if (parameterNames == null || args == null) {
            return null;
        }
        for (int i = 0; i < parameterNames.length; i++) {
            if ("userId".equals(parameterNames[i]) && args[i] instanceof String) {
                return (String) args[i];
            }
        }
        return null;
    }
}
