package com.easypan.controller;

import com.easypan.annotation.FileAccessCheck;
import com.easypan.component.RedisComponent;
import com.easypan.entity.config.AppConfig;
import com.easypan.entity.constants.Constants;
import com.easypan.entity.dto.DownloadFileDto;
import com.easypan.entity.enums.FileCategoryEnums;
import com.easypan.entity.enums.FileFolderTypeEnums;
import com.easypan.entity.enums.ResponseCodeEnum;
import com.easypan.entity.po.FileInfo;
import com.easypan.entity.query.FileInfoQuery;
import com.easypan.entity.vo.FolderVO;
import com.easypan.entity.vo.ResponseVO;
import com.easypan.exception.BusinessException;
import com.easypan.service.FileInfoService;
import com.easypan.utils.CopyTools;
import com.easypan.utils.StringTools;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.util.List;

/**
 * 公共文件控制器类.
 */
@Slf4j
public class CommonFileController extends ABaseController {

    @Resource
    protected FileInfoService fileInfoService;

    @Resource
    protected AppConfig appConfig;

    @Resource
    private RedisComponent redisComponent;

    /**
     * 获取文件夹信息.
     *
     * @param path   路径
     * @param userId 用户ID
     * @return 文件夹列表
     */
    public ResponseVO<List<FolderVO>> getFolderInfo(String path, String userId) {
        String[] pathArray = path.split("/");
        FileInfoQuery infoQuery = new FileInfoQuery();
        infoQuery.setUserId(userId);
        infoQuery.setFolderType(FileFolderTypeEnums.FOLDER.getType());
        infoQuery.setFileIdArray(pathArray);
        String orderBy = "field(file_id,\"" + StringUtils.join(pathArray, "\",\"") + "\")";
        infoQuery.setOrderBy(orderBy);
        List<FileInfo> fileInfoList = fileInfoService.findListByParam(infoQuery);
        return getSuccessResponseVO(CopyTools.copyList(fileInfoList, FolderVO.class));
    }

    /**
     * 获取图片.
     *
     * @param response    HTTP 响应
     * @param imageFolder 图片文件夹
     * @param imageName   图片名称
     * @param userId      用户ID
     */
    public void getImage(HttpServletResponse response, String imageFolder, String imageName, String userId) {
        if (StringTools.isEmpty(imageFolder) || StringUtils.isBlank(imageName)) {
            return;
        }

        if (!validateImageAccess(imageFolder, imageName, userId)) {
            log.warn("[IMAGE_ACCESS] Unauthorized access attempt: imageFolder={}, imageName={}, userId={}",
                    imageFolder, imageName, userId);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String imageSuffix = StringTools.getFileSuffix(imageName);
        imageSuffix = imageSuffix.replace(".", "");
        String contentType = "image/" + imageSuffix;
        response.setContentType(contentType);
        response.setHeader("Cache-Control", "max-age=2592000");
        String filePath = imageFolder + "/" + imageName;
        readFile(response, filePath);
    }

    private boolean validateImageAccess(String imageFolder, String imageName, String userId) {
        if (userId == null) {
            return false;
        }

        String imageNameWithoutSuffix = StringTools.getFileNameNoSuffix(imageName);
        if (imageNameWithoutSuffix == null) {
            return false;
        }

        // Check if the file belongs to the current user (fast check)
        if (imageNameWithoutSuffix.startsWith(userId)) {
            return true;
        }

        String ownerId = extractOwnerIdFromImageName(imageNameWithoutSuffix);
        if (ownerId == null) {
            return false;
        }

        if (ownerId.equals(userId)) {
            return true;
        }

        String fileId = extractFileIdFromImageName(imageNameWithoutSuffix, ownerId);
        if (fileId != null) {
            FileInfoQuery query = new FileInfoQuery();
            query.setFileId(fileId);
            query.setUserId(userId);
            Integer count = fileInfoService.findCountByParam(query);
            return count != null && count > 0;
        }

        return false;
    }

    private String extractOwnerIdFromImageName(String imageName) {
        if (StringUtils.isEmpty(imageName)) {
            return null;
        }
        // Support 32-char UUID or 10-char ID
        if (imageName.length() >= Constants.LENGTH_30) {
            return imageName.substring(0, Constants.LENGTH_30);
        }
        if (imageName.length() >= Constants.LENGTH_10) {
            return imageName.substring(0, Constants.LENGTH_10);
        }
        return null;
    }

    private String extractFileIdFromImageName(String imageName, String ownerId) {
        if (imageName == null || ownerId == null) {
            return null;
        }
        if (imageName.startsWith(ownerId) && imageName.length() > ownerId.length()) {
            return imageName.substring(ownerId.length());
        }
        return null;
    }

    /**
     * 获取文件.
     *
     * @param response HTTP 响应
     * @param fileId   文件ID
     * @param userId   用户ID
     */
    @FileAccessCheck
    protected void getFile(HttpServletResponse response, String fileId, String userId) {
        String filePath = null;
        if (fileId.endsWith(".ts")) {
            String[] tsAarray = fileId.split("_");
            String realFileId = tsAarray[0];
            // 根据原文件的id查询出一个文件集合
            FileInfo fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(realFileId, userId);
            if (fileInfo == null) {
                // 分享的视频，ts路径记录的是原视频的id,这里通过id直接取出原视频
                FileInfoQuery fileInfoQuery = new FileInfoQuery();
                fileInfoQuery.setFileId(realFileId);
                List<FileInfo> fileInfoList = fileInfoService.findListByParam(fileInfoQuery);
                fileInfo = fileInfoList.get(0);
                if (fileInfo == null) {
                    return;
                }

                // 根据当前用户id和路径去查询当前用户是否有该文件，如果没有直接返回
                fileInfoQuery = new FileInfoQuery();
                fileInfoQuery.setFilePath(fileInfo.getFilePath());
                fileInfoQuery.setUserId(userId);
                Integer count = fileInfoService.findCountByParam(fileInfoQuery);
                if (count == 0) {
                    return;
                }
            }
            String fileName = fileInfo.getFilePath();
            fileName = StringTools.getFileNameNoSuffix(fileName) + "/" + fileId;
            filePath = fileName;
        } else {
            FileInfo fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(fileId, userId);
            if (fileInfo == null) {
                return;
            }
            // 视频文件读取.m3u8文件
            if (FileCategoryEnums.VIDEO.getCategory().equals(fileInfo.getFileCategory())) {
                // 重新设置文件路径
                String fileNameNoSuffix = StringTools.getFileNameNoSuffix(fileInfo.getFilePath());
                filePath = fileNameNoSuffix + "/" + Constants.M3U8_NAME;
            } else {
                filePath = fileInfo.getFilePath();
            }
        }

        readFile(response, filePath);
    }

    /**
     * 创建下载链接.
     *
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 下载码
     */
    @FileAccessCheck
    protected ResponseVO<String> createDownloadUrl(String fileId, String userId) {
        FileInfo fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(fileId, userId);
        if (fileInfo == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600.getCode(), "文件不存在或无权访问");
        }
        if (FileFolderTypeEnums.FOLDER.getType().equals(fileInfo.getFolderType())) {
            throw new BusinessException(ResponseCodeEnum.CODE_600.getCode(), "文件夹不支持下载，请选择文件");
        }
        String code = StringTools.getRandomString(Constants.LENGTH_50);
        DownloadFileDto downloadFileDto = new DownloadFileDto();
        downloadFileDto.setDownloadCode(code);
        downloadFileDto.setFilePath(fileInfo.getFilePath());
        downloadFileDto.setFileName(fileInfo.getFileName());

        redisComponent.saveDownloadCode(code, downloadFileDto);

        return getSuccessResponseVO(code);
    }

    /**
     * 下载文件.
     *
     * @param request  HTTP 请求
     * @param response HTTP 响应
     * @param code     下载码
     * @throws Exception 异常
     */
    protected void download(HttpServletRequest request, HttpServletResponse response, String code) throws Exception {
        long startTime = System.currentTimeMillis();
        String fileName = "unknown";

        try {
            DownloadFileDto downloadFileDto = redisComponent.getDownloadCode(code);
            if (null == downloadFileDto) {
                log.warn("[DOWNLOAD] Invalid download code: {}", code);
                return;
            }

            fileName = downloadFileDto.getFileName();

            log.info("[DOWNLOAD] Start downloading file: {} (code: {})", fileName, code);

            response.setContentType("application/octet-stream");
            response.setCharacterEncoding("UTF-8");
            response.setBufferSize(8192);

            String filePath = downloadFileDto.getFilePath();

            if (request.getHeader("User-Agent").toLowerCase().indexOf("msie") > 0) {
                fileName = URLEncoder.encode(fileName, "UTF-8");
            } else {
                fileName = new String(fileName.getBytes("UTF-8"), "ISO8859-1");
            }
            response.setHeader("Content-Disposition", "attachment;filename=\"" + fileName + "\"");

            readFile(response, filePath);

            long duration = System.currentTimeMillis() - startTime;
            log.info("[DOWNLOAD] Successfully downloaded file: {} in {}ms", fileName, duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[DOWNLOAD] Failed to download file: {} after {}ms, error: {}",
                    fileName, duration, e.getMessage(), e);
            throw e;
        }
    }
}
