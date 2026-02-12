package com.easypan.controller;

import com.easypan.annotation.GlobalInterceptor;
import com.easypan.annotation.VerifyParam;
import com.easypan.entity.constants.Constants;
import com.easypan.entity.dto.SessionShareDto;
import com.easypan.entity.dto.SessionWebUserDto;
import com.easypan.entity.enums.FileDelFlagEnums;
import com.easypan.entity.enums.ResponseCodeEnum;
import com.easypan.entity.po.FileInfo;
import com.easypan.entity.po.FileShare;
import com.easypan.entity.po.UserInfo;
import com.easypan.entity.query.FileInfoQuery;
import com.easypan.entity.vo.FileInfoVO;
import com.easypan.entity.vo.FolderVO;
import com.easypan.entity.vo.PaginationResultVO;
import com.easypan.entity.vo.ResponseVO;
import com.easypan.entity.vo.ShareInfoVO;
import com.easypan.exception.BusinessException;
import com.easypan.metrics.CustomMetrics;
import com.easypan.service.FileInfoService;
import com.easypan.service.FileShareService;
import com.easypan.service.ShareAccessLogService;
import com.easypan.service.UserInfoService;
import com.easypan.utils.CopyTools;
import com.easypan.utils.StringTools;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.Date;
import java.util.List;

@RestController("webShareController")
@RequestMapping("/showShare")
public class WebShareController extends CommonFileController {

    private static final String ACCESS_TYPE_VIEW = "VIEW";
    private static final String ACCESS_TYPE_CHECK_CODE = "CHECK_CODE";

    @Resource
    private FileShareService fileShareService;

    @Resource
    private FileInfoService fileInfoService;

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private ShareAccessLogService shareAccessLogService;

    @Resource
    private CustomMetrics customMetrics;

    /**
     * 通过分享ID获取分享文件信息
     */
    private ShareInfoVO getShareInfoCommon(String shareId) {
        FileShare share = fileShareService.getFileShareByShareId(shareId);
        if (null == share || (share.getExpireTime() != null && new Date().after(share.getExpireTime()))) {
            throw new BusinessException(ResponseCodeEnum.CODE_902.getMsg());
        }
        ShareInfoVO shareInfoVO = CopyTools.copy(share, ShareInfoVO.class);
        FileInfo fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(share.getFileId(), share.getUserId());
        if (fileInfo == null || !FileDelFlagEnums.USING.getFlag().equals(fileInfo.getDelFlag())) {
            throw new BusinessException(ResponseCodeEnum.CODE_902.getMsg());
        }
        shareInfoVO.setFileName(fileInfo.getFileName());
        UserInfo userInfo = userInfoService.getUserInfoByUserId(share.getUserId());
        shareInfoVO.setNickName(userInfo.getNickName());
        shareInfoVO.setAvatar(userInfo.getQqAvatar());
        shareInfoVO.setUserId(userInfo.getUserId());
        return shareInfoVO;
    }

    /**
     * 校验分享是否失效
     *
     * @param session
     * @param shareId
     * @return
     */
    private SessionShareDto checkShare(HttpSession session, String shareId) {
        SessionShareDto shareSessionDto = getSessionShareFromSession(session, shareId);
        if (shareSessionDto == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_903);
        }
        if (shareSessionDto.getExpireTime() != null && new Date().after(shareSessionDto.getExpireTime())) {
            throw new BusinessException(ResponseCodeEnum.CODE_902);
        }
        return shareSessionDto;
    }

    /**
     * 获取分享登录信息
     *
     * @param session
     * @param shareId
     * @return
     */
    @RequestMapping("/getShareLoginInfo")
    @GlobalInterceptor(checkLogin = false, checkParams = true)
    public ResponseVO<ShareInfoVO> getShareLoginInfo(HttpSession session, @VerifyParam(required = true) String shareId) {
        SessionShareDto shareSessionDto = getSessionShareFromSession(session, shareId);
        if (shareSessionDto == null) {
            return getSuccessResponseVO(null);
        }
        ShareInfoVO shareInfoVO = getShareInfoCommon(shareId);
        // 判断是否是当前用户分享的文件
        SessionWebUserDto userDto = getUserInfoFromSession(session);
        if (userDto != null && userDto.getUserId().equals(shareSessionDto.getShareUserId())) {
            shareInfoVO.setCurrentUser(true);
        } else {
            shareInfoVO.setCurrentUser(false);
        }
        return getSuccessResponseVO(shareInfoVO);
    }

    /**
     * 获取分享信息
     *
     * @param shareId
     * @return
     */
    @RequestMapping("/getShareInfo")
    @GlobalInterceptor(checkLogin = false, checkParams = true)
    public ResponseVO<ShareInfoVO> getShareInfo(@VerifyParam(required = true) String shareId) {
        return getSuccessResponseVO(getShareInfoCommon(shareId));
    }

    /**
     * 校验分享码
     *
     * @param session
     * @param shareId
     * @param code
     * @return
     */
    @RequestMapping("/checkShareCode")
    @GlobalInterceptor(checkLogin = false, checkParams = true)
    public ResponseVO<Void> checkShareCode(HttpSession session, HttpServletRequest request,
            @VerifyParam(required = true) String shareId,
            @VerifyParam(required = true) String code) {
        String visitorId = getVisitorId(session);
        String visitorIp = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        
        try {
            SessionShareDto shareSessionDto = fileShareService.checkShareCode(shareId, code);
            session.setAttribute(Constants.SESSION_SHARE_KEY + shareId, shareSessionDto);
            
            shareAccessLogService.logAccessAsync(shareId, null, visitorId, visitorIp, userAgent, 
                    ACCESS_TYPE_CHECK_CODE, true, null);
            customMetrics.incrementShareAccess();
            return getSuccessResponseVO(null);
        } catch (BusinessException e) {
            shareAccessLogService.logAccessAsync(shareId, null, visitorId, visitorIp, userAgent, 
                    ACCESS_TYPE_CHECK_CODE, false, e.getMessage());
            throw e;
        }
    }

    /**
     * 获取文件列表
     *
     * @param session
     * @param shareId
     * @return
     */
    @RequestMapping("/loadFileList")
    @GlobalInterceptor(checkLogin = false, checkParams = true)
    public ResponseVO<PaginationResultVO<FileInfoVO>> loadFileList(HttpSession session,
            @VerifyParam(required = true) String shareId, String filePid) {
        SessionShareDto shareSessionDto = checkShare(session, shareId);
        FileInfoQuery query = new FileInfoQuery();
        if (!StringTools.isEmpty(filePid) && !Constants.ZERO_STR.equals(filePid)) {
            fileInfoService.checkRootFilePid(shareSessionDto.getFileId(), shareSessionDto.getShareUserId(), filePid);
            query.setFilePid(filePid);
        } else {
            query.setFileId(shareSessionDto.getFileId());
        }
        query.setUserId(shareSessionDto.getShareUserId());
        query.setOrderBy("last_update_time desc");
        query.setDelFlag(FileDelFlagEnums.USING.getFlag());
        PaginationResultVO<FileInfo> resultVO = fileInfoService.findListByPage(query);
        return getSuccessResponseVO(convert2PaginationVO(resultVO, FileInfoVO.class));
    }

    /**
     * 获取目录信息
     *
     * @param session
     * @param shareId
     * @param path
     * @return
     */
    @RequestMapping("/getFolderInfo")
    @GlobalInterceptor(checkLogin = false, checkParams = true)
    public ResponseVO<List<FolderVO>> getFolderInfo(HttpSession session,
            @VerifyParam(required = true) String shareId,
            @VerifyParam(required = true) String path) {
        SessionShareDto shareSessionDto = checkShare(session, shareId);
        return super.getFolderInfo(path, shareSessionDto.getShareUserId());
    }

    @RequestMapping("/getFile/{shareId}/{fileId}")
    public void getFile(HttpServletResponse response, HttpSession session, HttpServletRequest request,
            @PathVariable("shareId") @VerifyParam(required = true) String shareId,
            @PathVariable("fileId") @VerifyParam(required = true) String fileId) {
        SessionShareDto shareSessionDto = checkShare(session, shareId);
        
        String visitorId = getVisitorId(session);
        String visitorIp = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        shareAccessLogService.logAccessAsync(shareId, fileId, visitorId, visitorIp, userAgent, 
                ACCESS_TYPE_VIEW, true, null);
        
        super.getFile(response, fileId, shareSessionDto.getShareUserId());
    }

    @RequestMapping("/getImage/{shareId}/{imageFolder}/{imageName}")
    @GlobalInterceptor(checkLogin = false, checkParams = true)
    public void getImage(HttpSession session, HttpServletResponse response,
            @PathVariable("shareId") @VerifyParam(required = true) String shareId,
            @PathVariable("imageFolder") String imageFolder,
            @PathVariable("imageName") String imageName) {
        SessionShareDto shareSessionDto = checkShare(session, shareId);
        super.getImage(response, imageFolder, imageName, shareSessionDto.getShareUserId());
    }

    @RequestMapping("/ts/getVideoInfo/{shareId}/{fileId}")
    public void getVideoInfo(HttpServletResponse response,
            HttpSession session,
            @PathVariable("shareId") @VerifyParam(required = true) String shareId,
            @PathVariable("fileId") @VerifyParam(required = true) String fileId) {
        SessionShareDto shareSessionDto = checkShare(session, shareId);
        super.getFile(response, fileId, shareSessionDto.getShareUserId());
    }

    @RequestMapping("/createDownloadUrl/{shareId}/{fileId}")
    @GlobalInterceptor(checkLogin = false, checkParams = true)
    public ResponseVO<String> createDownloadUrl(HttpSession session,
            @PathVariable("shareId") @VerifyParam(required = true) String shareId,
            @PathVariable("fileId") @VerifyParam(required = true) String fileId) {
        SessionShareDto shareSessionDto = checkShare(session, shareId);
        return super.createDownloadUrl(fileId, shareSessionDto.getShareUserId());
    }

    /**
     * 下载
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping("/download/{code}")
    @GlobalInterceptor(checkLogin = false, checkParams = true)
    public void download(HttpServletRequest request, HttpServletResponse response,
            @PathVariable("code") @VerifyParam(required = true) String code) throws Exception {
        super.download(request, response, code);
    }

    /**
     * 保存分享
     *
     * @param session
     * @param shareId
     * @param shareFileIds
     * @param myFolderId
     * @return
     */
    @RequestMapping("/saveShare")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO<Void> saveShare(HttpSession session,
            @VerifyParam(required = true) String shareId,
            @VerifyParam(required = true) String shareFileIds,
            @VerifyParam(required = true) String myFolderId) {
        SessionShareDto shareSessionDto = checkShare(session, shareId);
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        if (shareSessionDto.getShareUserId().equals(webUserDto.getUserId())) {
            throw new BusinessException("自己分享的文件无法保存到自己的网盘");
        }
        fileInfoService.saveShare(shareSessionDto.getFileId(), shareFileIds, myFolderId,
                shareSessionDto.getShareUserId(), webUserDto.getUserId());
        return getSuccessResponseVO(null);
    }

    private String getVisitorId(HttpSession session) {
        SessionWebUserDto userDto = getUserInfoFromSession(session);
        return userDto != null ? userDto.getUserId() : null;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
