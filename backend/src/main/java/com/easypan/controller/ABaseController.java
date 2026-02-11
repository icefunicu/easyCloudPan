package com.easypan.controller;

import com.easypan.entity.constants.Constants;
import com.easypan.entity.dto.SessionShareDto;
import com.easypan.entity.dto.SessionWebUserDto;
import com.easypan.entity.enums.ResponseCodeEnum;
import com.easypan.entity.vo.PaginationResultVO;
import com.easypan.entity.vo.ResponseVO;
import com.easypan.utils.CopyTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class ABaseController {

    private static final Logger logger = LoggerFactory.getLogger(ABaseController.class);
    private static final int STREAM_BUFFER_SIZE = 64 * 1024;
    private static final long TRANSFER_CHUNK_SIZE = 8L * 1024 * 1024;

    @Resource
    private com.easypan.component.S3Component s3Component;

    protected static final String STATUC_SUCCESS = "success";

    protected static final String STATUC_ERROR = "error";

    protected <T> ResponseVO<T> getSuccessResponseVO(T t) {
        ResponseVO<T> responseVO = new ResponseVO<>();
        responseVO.setStatus(STATUC_SUCCESS);
        responseVO.setCode(ResponseCodeEnum.CODE_200.getCode());
        responseVO.setInfo(ResponseCodeEnum.CODE_200.getMsg());
        responseVO.setData(t);
        return responseVO;
    }

    protected <S, T> PaginationResultVO<T> convert2PaginationVO(PaginationResultVO<S> result, Class<T> classz) {
        PaginationResultVO<T> resultVO = new PaginationResultVO<>();
        resultVO.setList(CopyTools.copyList(result.getList(), classz));
        resultVO.setPageNo(result.getPageNo());
        resultVO.setPageSize(result.getPageSize());
        resultVO.setPageTotal(result.getPageTotal());
        resultVO.setTotalCount(result.getTotalCount());
        return resultVO;
    }

    protected SessionWebUserDto getUserInfoFromSession(HttpSession session) {
        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        return sessionWebUserDto;
    }

    protected SessionShareDto getSessionShareFromSession(HttpSession session, String shareId) {
        SessionShareDto sessionShareDto = (SessionShareDto) session.getAttribute(Constants.SESSION_SHARE_KEY + shareId);
        return sessionShareDto;
    }

    protected void readFile(HttpServletResponse response, String filePath) {
        OutputStream out = null;
        try {
            response.setBufferSize(STREAM_BUFFER_SIZE);
            out = response.getOutputStream();
            if (isLocalPath(filePath)) {
                transferLocalFile(filePath, out);
            } else {
                transferStreamFromS3(filePath, out);
            }
            out.flush();
        } catch (Exception e) {
            logger.error("Read file failed, path: {}", filePath, e);
        }
    }

    private boolean isLocalPath(String filePath) {
        return filePath.startsWith("/") || (filePath.contains(":") && filePath.indexOf(":") < 3);
    }

    private void transferLocalFile(String filePath, OutputStream out) throws IOException {
        Path path = new File(filePath).toPath();
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            return;
        }
        try (FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.READ)) {
            WritableByteChannel outChannel = Channels.newChannel(out);
            long size = fileChannel.size();
            long position = 0L;
            while (position < size) {
                long transferred = fileChannel.transferTo(position, Math.min(TRANSFER_CHUNK_SIZE, size - position), outChannel);
                if (transferred <= 0) {
                    transferRemainingByBuffer(fileChannel, position, out);
                    break;
                }
                position += transferred;
            }
        }
    }

    private void transferRemainingByBuffer(FileChannel fileChannel, long position, OutputStream out) throws IOException {
        fileChannel.position(position);
        byte[] buffer = new byte[STREAM_BUFFER_SIZE];
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
        int read;
        while ((read = fileChannel.read(byteBuffer)) != -1) {
            out.write(buffer, 0, read);
            byteBuffer.clear();
        }
    }

    private void transferStreamFromS3(String filePath, OutputStream out) throws IOException {
        try (InputStream in = s3Component.getInputStream(filePath)) {
            if (in == null) {
                return;
            }
            byte[] buffer = new byte[STREAM_BUFFER_SIZE];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        }
    }
}

