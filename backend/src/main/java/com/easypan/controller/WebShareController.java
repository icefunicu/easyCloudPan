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
import com.easypan.annotation.RateLimit;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.Date;
import java.util.List;

/**
 * Web分享控制器类，处理分享文件的Web访问操作.
 */
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
     * 通过分享ID获取分享文件信息.
     *
     * @param shareId 分享ID
     * @return 分享信息
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
     * 校验分享是否失效.
     *
     * @param session HTTP 会话
     * @param shareId 分享ID
     * @return 分享会话信息
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
     * 获取分享登录信息.
     *
     * @param session HTTP 会话
     * @param shareId 分享ID
     * @return 分享登录信息
     */
    @RequestMapping("/getShareLoginInfo")
    @GlobalInterceptor(checkLogin = false, checkParams = true)
    public ResponseVO<ShareInfoVO> getShareLoginInfo(HttpSession session,
            @VerifyParam(required = true) String shareId) {
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
     * 获取分享信息.
     *
     * @param shareId 分享ID
     * @return 分享信息
     */
    @RequestMapping("/getShareInfo")
    @GlobalInterceptor(checkLogin = false, checkParams = true)
    public ResponseVO<ShareInfoVO> getShareInfo(@VerifyParam(required = true) String shareId) {
        return getSuccessResponseVO(getShareInfoCommon(shareId));
    }

    /**
     * 校验分享码.
     *
     * @param session HTTP 会话
     * @param request HTTP 请求
     * @param shareId 分享ID
     * @param code    分享码
     * @return 响应对象
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
     * 获取文件列表.
     *
     * @param session HTTP 会话
     * @param shareId 分享ID
     * @param filePid 文件父ID
     * @return 文件列表
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
     * 获取目录信息.
     *
     * @param session HTTP 会话
     * @param shareId 分享ID
     * @param path    路径
     * @return 目录信息列表
     */
    @RequestMapping("/getFolderInfo")
    @GlobalInterceptor(checkLogin = false, checkParams = true)
    public ResponseVO<List<FolderVO>> getFolderInfo(HttpSession session,
            @VerifyParam(required = true) String shareId,
            @VerifyParam(required = true) String path) {
        SessionShareDto shareSessionDto = checkShare(session, shareId);
        return super.getFolderInfo(path, shareSessionDto.getShareUserId());
    }

    /**
     * 获取文件.
     *
     * @param response HTTP 响应
     * @param session  HTTP 会话
     * @param request  HTTP 请求
     * @param shareId  分享ID
     * @param fileId   文件ID
     */
    @RequestMapping("/getFile/{shareId}/{fileId}")
    @RateLimit(time = 1, count = 20)
    public void getFile(HttpServletResponse response, HttpSession session, HttpServletRequest request,
            @PathVariable("shareId") @VerifyParam(required = true) String shareId,
            @PathVariable("fileId") @VerifyParam(required = true) String fileId) {
        checkReferer(request);
        SessionShareDto shareSessionDto = checkShare(session, shareId);

        String visitorId = getVisitorId(session);
        String visitorIp = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        shareAccessLogService.logAccessAsync(shareId, fileId, visitorId, visitorIp, userAgent,
                ACCESS_TYPE_VIEW, true, null);

        super.getFile(response, fileId, shareSessionDto.getShareUserId());
    }

    /**
     * 获取图片.
     *
     * @param session     HTTP 会话
     * @param request     HTTP 请求
     * @param response    HTTP 响应
     * @param shareId     分享ID
     * @param imageFolder 图片文件夹
     * @param imageName   图片名称
     */
    @RequestMapping("/getImage/{shareId}/{imageFolder}/{imageName}")
    @GlobalInterceptor(checkLogin = false, checkParams = true)
    @RateLimit(time = 1, count = 30)
    public void getImage(HttpSession session, HttpServletRequest request, HttpServletResponse response,
            @PathVariable("shareId") @VerifyParam(required = true) String shareId,
            @PathVariable("imageFolder") String imageFolder,
            @PathVariable("imageName") String imageName) {
        checkReferer(request);
        SessionShareDto shareSessionDto = checkShare(session, shareId);
        super.getImage(response, imageFolder, imageName, shareSessionDto.getShareUserId());
    }

    /**
     * 获取视频.
     *
     * @param response HTTP 响应
     * @param request  HTTP 请求
     * @param session  HTTP 会话
     * @param shareId  分享ID
     * @param fileId   文件ID
     */
    @RequestMapping("/ts/getVideoInfo/{shareId}/{fileId}")
    @RateLimit(time = 1, count = 100)
    public void getVideoInfo(HttpServletResponse response, HttpServletRequest request,
            HttpSession session,
            @PathVariable("shareId") @VerifyParam(required = true) String shareId,
            @PathVariable("fileId") @VerifyParam(required = true) String fileId) {
        checkReferer(request);
        SessionShareDto shareSessionDto = checkShare(session, shareId);
        super.getFile(response, fileId, shareSessionDto.getShareUserId());
    }

    /**
     * 创建下载链接.
     *
     * @param session HTTP 会话
     * @param request HTTP 请求
     * @param shareId 分享ID
     * @param fileId  文件ID
     * @return 下载链接
     */
    @RequestMapping("/createDownloadUrl/{shareId}/{fileId}")
    @GlobalInterceptor(checkLogin = false, checkParams = true)
    @RateLimit(time = 1, count = 5)
    public ResponseVO<String> createDownloadUrl(HttpSession session, HttpServletRequest request,
            @PathVariable("shareId") @VerifyParam(required = true) String shareId,
            @PathVariable("fileId") @VerifyParam(required = true) String fileId) {
        checkReferer(request);
        SessionShareDto shareSessionDto = checkShare(session, shareId);
        return super.createDownloadUrl(fileId, shareSessionDto.getShareUserId());
    }

    /**
     * 下载文件.
     *
     * @param request  HTTP 请求
     * @param response HTTP 响应
     * @param code     下载码
     * @throws Exception 异常
     */
    @RequestMapping("/download/{code}")
    @GlobalInterceptor(checkLogin = false, checkParams = true)
    public void download(HttpServletRequest request, HttpServletResponse response,
            @PathVariable("code") @VerifyParam(required = true) String code) throws Exception {
        super.download(request, response, code);
    }

    /**
     * 保存分享文件到自己的网盘.
     *
     * @param session      HTTP 会话
     * @param shareId      分享ID
     * @param shareFileIds 分享文件ID列表
     * @param myFolderId   目标文件夹ID
     * @return 响应对象
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

    private void checkReferer(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        // 如果存在 Referer 则拦截陌生域，白名单可以后期提至配置中心
        if (!StringTools.isEmpty(referer)) {
            if (!referer.contains("localhost") && !referer.contains("127.0.0.1")
                    && !referer.contains("easycloudpan.com")) {
                throw new BusinessException(ResponseCodeEnum.CODE_600.getCode(), "请求非法，触发防盗链拦截");
            }
        }
    }
}
