package com.easypan.controller;

import com.easypan.entity.constants.Constants;
import com.easypan.entity.dto.SessionShareDto;
import com.easypan.entity.dto.SessionWebUserDto;
import com.easypan.entity.enums.ResponseCodeEnum;
import com.easypan.entity.vo.PaginationResultVO;
import com.easypan.entity.vo.ResponseVO;
import com.easypan.exception.BusinessException;
import com.easypan.utils.CopyTools;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.util.Map;

/**
 * 基础控制器类.
 */
public class ABaseController {

    private static final Logger logger = LoggerFactory.getLogger(ABaseController.class);
    private static final int STREAM_BUFFER_SIZE = 64 * 1024;

    /** 常见媒体类型映射（补充 JDK 内置映射的不足）. */
    private static final Map<String, String> MEDIA_TYPE_MAP = Map.ofEntries(
            Map.entry("m3u8", "application/vnd.apple.mpegurl"),
            Map.entry("ts", "video/mp2t"),
            Map.entry("mp4", "video/mp4"),
            Map.entry("webm", "video/webm"),
            Map.entry("mkv", "video/x-matroska"),
            Map.entry("flv", "video/x-flv"),
            Map.entry("avi", "video/x-msvideo"),
            Map.entry("mov", "video/quicktime"),
            Map.entry("mp3", "audio/mpeg"),
            Map.entry("flac", "audio/flac"),
            Map.entry("ogg", "audio/ogg"),
            Map.entry("wav", "audio/wav"),
            Map.entry("aac", "audio/aac"),
            Map.entry("webp", "image/webp"),
            Map.entry("svg", "image/svg+xml"),
            Map.entry("pdf", "application/pdf"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

    /** 需要长缓存的静态资源扩展名（30 天）. */
    private static final String CACHE_LONG = "public, max-age=2592000, immutable";
    /** 动态内容不缓存. */
    private static final String CACHE_NO = "no-cache, no-store, must-revalidate";
    /** 短缓存（5 分钟）用于一般文件预览. */
    private static final String CACHE_SHORT = "public, max-age=300";

    @Resource
    protected com.easypan.strategy.StorageFactory storageFactory;

    protected static final String STATUC_SUCCESS = "success";

    protected static final String STATUC_ERROR = "error";

    /**
     * 获取成功响应.
     *
     * @param t   响应数据
     * @param <T> 数据类型
     * @return 响应对象
     */
    protected <T> ResponseVO<T> getSuccessResponseVO(T t) {
        ResponseVO<T> responseVO = new ResponseVO<>();
        responseVO.setStatus(STATUC_SUCCESS);
        responseVO.setCode(ResponseCodeEnum.CODE_200.getCode());
        responseVO.setInfo(ResponseCodeEnum.CODE_200.getMsg());
        responseVO.setData(t);
        return responseVO;
    }

    /**
     * 转换分页结果.
     *
     * @param result 原始分页结果
     * @param classz 目标类型
     * @param <S>    源类型
     * @param <T>    目标类型
     * @return 转换后的分页结果
     */
    protected <S, T> PaginationResultVO<T> convert2PaginationVO(PaginationResultVO<S> result, Class<T> classz) {
        PaginationResultVO<T> resultVO = new PaginationResultVO<>();
        resultVO.setList(CopyTools.copyList(result.getList(), classz));
        resultVO.setPageNo(result.getPageNo());
        resultVO.setPageSize(result.getPageSize());
        resultVO.setPageTotal(result.getPageTotal());
        resultVO.setTotalCount(result.getTotalCount());
        return resultVO;
    }

    /**
     * 从会话获取用户信息.
     *
     * @param session HTTP 会话
     * @return 用户信息
     */
    protected SessionWebUserDto getUserInfoFromSession(HttpSession session) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                .getRequest();
        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto) request.getAttribute(Constants.SESSION_KEY);
        if (sessionWebUserDto == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }
        return sessionWebUserDto;
    }

    /**
     * 从会话获取分享信息.
     *
     * @param session HTTP 会话
     * @param shareId 分享ID
     * @return 分享信息
     */
    protected SessionShareDto getSessionShareFromSession(HttpSession session, String shareId) {
        SessionShareDto sessionShareDto = (SessionShareDto) session.getAttribute(Constants.SESSION_SHARE_KEY + shareId);
        return sessionShareDto;
    }

    /**
     * 根据文件路径推断 Content-Type.
     *
     * @param filePath 文件路径
     * @return MIME 类型，无法推断时返回 application/octet-stream
     */
    protected String guessContentType(String filePath) {
        if (filePath == null) {
            return "application/octet-stream";
        }
        String ext = "";
        int dotIndex = filePath.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < filePath.length() - 1) {
            ext = filePath.substring(dotIndex + 1).toLowerCase();
        }
        // 优先从自定义映射查找
        String type = MEDIA_TYPE_MAP.get(ext);
        if (type != null) {
            return type;
        }
        // 回退到 JDK 内置推断
        type = URLConnection.guessContentTypeFromName(filePath);
        return type != null ? type : "application/octet-stream";
    }

    /**
     * 根据文件扩展名选择缓存策略.
     *
     * @param filePath 文件路径
     * @return Cache-Control 头的值
     */
    protected String chooseCacheControl(String filePath) {
        if (filePath == null) {
            return CACHE_NO;
        }
        // m3u8 播放列表是动态内容，不应该缓存
        if (filePath.endsWith(".m3u8")) {
            return CACHE_NO;
        }
        // ts 分片、图片等静态资源可以长缓存
        if (filePath.endsWith(".ts") || filePath.endsWith(".jpg") || filePath.endsWith(".jpeg")
                || filePath.endsWith(".png") || filePath.endsWith(".gif") || filePath.endsWith(".webp")) {
            return CACHE_LONG;
        }
        // 其他文件预览使用短缓存
        return CACHE_SHORT;
    }

    /**
     * 读取文件并写入响应.
     * 自动设置 Content-Type 和 Cache-Control 头.
     *
     * @param response HTTP 响应
     * @param filePath 文件路径
     */
    protected void readFile(HttpServletResponse response, String filePath) {
        OutputStream out = null;
        InputStream in = null;
        try {
            // 设置 Content-Type（如果尚未由上层调用者设置）
            if (response.getContentType() == null) {
                response.setContentType(guessContentType(filePath));
            }
            // 设置缓存策略
            if (response.getHeader("Cache-Control") == null) {
                response.setHeader("Cache-Control", chooseCacheControl(filePath));
            }

            response.setBufferSize(STREAM_BUFFER_SIZE);
            out = response.getOutputStream();
            in = storageFactory.getStorageStrategy().download(filePath);
            if (in == null) {
                return;
            }
            byte[] buffer = new byte[STREAM_BUFFER_SIZE];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            out.flush();
        } catch (Exception e) {
            logger.error("Read file failed, path: {}", filePath, e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    logger.error("Close input stream failed", e);
                }
            }
        }
    }
}
