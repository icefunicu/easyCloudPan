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

/**
 * 基础控制器类.
 */
public class ABaseController {

    private static final Logger logger = LoggerFactory.getLogger(ABaseController.class);
    private static final int STREAM_BUFFER_SIZE = 64 * 1024;

    @Resource
    private com.easypan.strategy.StorageFactory storageFactory;

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
            // 兼容可能尚未挂载的情况（例如非鉴权接口直接请求时）
            sessionWebUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        }
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
     * 读取文件并写入响应.
     *
     * @param response HTTP 响应
     * @param filePath 文件路径
     */
    protected void readFile(HttpServletResponse response, String filePath) {
        OutputStream out = null;
        InputStream in = null;
        try {
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
