package com.easypan.controller;

import com.easypan.annotation.GlobalInterceptor;
import com.easypan.annotation.VerifyParam;
import com.easypan.component.RedisComponent;
import com.easypan.entity.config.AppConfig;
import com.easypan.entity.constants.Constants;
import com.easypan.entity.dto.CreateImageCode;
import com.easypan.entity.dto.SessionWebUserDto;
import com.easypan.entity.dto.UserSpaceDto;
import com.easypan.entity.enums.ResponseCodeEnum;
import com.easypan.entity.enums.VerifyRegexEnum;

import com.easypan.entity.po.UserInfo;
import com.easypan.entity.vo.ResponseVO;
import com.easypan.exception.BusinessException;
import com.easypan.service.EmailCodeService;
import com.easypan.service.UserInfoService;
import com.easypan.utils.StringTools;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * 账户控制器类.
 */
@RestController("accountController")
@Tag(name = "Account Management", description = "User account and authentication operations")
public class AccountController extends ABaseController {
    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_VALUE = "application/json;charset=UTF-8";

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private EmailCodeService emailCodeService;

    @Resource
    private AppConfig appConfig;

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private com.easypan.component.JwtTokenProvider jwtTokenProvider;

    @Resource
    private com.easypan.service.JwtBlacklistService jwtBlacklistService;

    /**
     * Local-only convenience for automation: when enabled, /checkCode responds with
     * the captcha code in a header.
     * Default is disabled for security.
     */
    @Value("${captcha.debug.header:false}")
    private boolean captchaDebugHeader;

    /**
     * 获取验证码.
     *
     * @param response HTTP 响应
     * @param session  HTTP 会话
     * @param type     验证码类型
     * @throws IOException IO 异常
     */
    @RequestMapping(value = "/checkCode")
    @Operation(summary = "Get Check Code", description = "Get graphic verification code")
    public void checkCode(HttpServletResponse response, HttpSession session, Integer type) throws IOException {
        CreateImageCode verifyCode = new CreateImageCode(130, 38, 5, 10);
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        // CreateImageCode.write() outputs PNG bytes.
        response.setContentType("image/png");
        String code = verifyCode.getCode();
        if (type == null || type == 0) {
            session.setAttribute(Constants.CHECK_CODE_KEY, code);
        } else {
            session.setAttribute(Constants.CHECK_CODE_KEY_EMAIL, code);
        }
        if (captchaDebugHeader) {
            response.setHeader("X-EasyPan-CheckCode", code);
        }
        verifyCode.write(response.getOutputStream());
    }

    /**
     * 发送邮箱验证码.
     *
     * @param session   HTTP 会话
     * @param email     邮箱地址
     * @param checkCode 验证码
     * @param type      类型
     * @return 响应对象
     */
    @RequestMapping("/sendEmailCode")
    @GlobalInterceptor(checkLogin = false, checkParams = true)
    @Operation(summary = "Send Email Code", description = "Send verification code to email")
    public ResponseVO<Void> sendEmailCode(HttpSession session,
            @VerifyParam(required = true, regex = VerifyRegexEnum.EMAIL, max = 150) String email,
            @VerifyParam(required = true) String checkCode,
            @VerifyParam(required = true) Integer type) {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY_EMAIL))) {
                throw new BusinessException("图片验证码不正确");
            }
            emailCodeService.sendEmailCode(email, type);
            return getSuccessResponseVO(null);
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY_EMAIL);
        }
    }

    /**
     * 用户注册.
     *
     * @param session   HTTP 会话
     * @param email     邮箱地址
     * @param nickName  昵称
     * @param password  密码
     * @param checkCode 验证码
     * @param emailCode 邮箱验证码
     * @return 响应对象
     */
    @RequestMapping("/register")
    @GlobalInterceptor(checkLogin = false, checkParams = true)
    @Operation(summary = "Register", description = "User registration")
    public ResponseVO<Void> register(HttpSession session,
            @VerifyParam(required = true, regex = VerifyRegexEnum.EMAIL, max = 150) String email,
            @VerifyParam(required = true, max = 20) String nickName,
            @VerifyParam(required = true, regex = VerifyRegexEnum.PASSWORD, min = 8, max = 32) String password,
            @VerifyParam(required = true) String checkCode,
            @VerifyParam(required = true) String emailCode) {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))) {
                throw new BusinessException("图片验证码不正确");
            }
            userInfoService.register(email, nickName, password, emailCode);
            return getSuccessResponseVO(null);
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }

    /**
     * 用户登录.
     *
     * @param session   HTTP 会话
     * @param request   HTTP 请求
     * @param email     邮箱地址
     * @param password  密码
     * @param checkCode 验证码
     * @return 响应对象
     */
    @RequestMapping("/login")
    @GlobalInterceptor(checkLogin = false, checkParams = true)
    @Operation(summary = "Login", description = "User login")
    public ResponseVO<Map<String, Object>> login(HttpSession session, HttpServletRequest request,
            @VerifyParam(required = true) String email,
            @VerifyParam(required = true) String password,
            @VerifyParam(required = true) String checkCode) {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))) {
                throw new BusinessException("图片验证码不正确");
            }
            SessionWebUserDto sessionWebUserDto = userInfoService.login(email, password);
            session.setAttribute(Constants.SESSION_KEY, sessionWebUserDto);

            String token = jwtTokenProvider.generateToken(sessionWebUserDto.getUserId(), null);
            String refreshToken = jwtTokenProvider.generateRefreshToken(sessionWebUserDto.getUserId(), null);

            long refreshExpirationSeconds = 2592000L;
            redisComponent.saveRefreshToken(sessionWebUserDto.getUserId(), refreshToken, refreshExpirationSeconds);

            Map<String, Object> result = new HashMap<>();
            result.put("userInfo", sessionWebUserDto);
            result.put("token", token);
            result.put("refreshToken", refreshToken);

            return getSuccessResponseVO(result);
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }

    /**
     * 重置密码.
     *
     * @param session   HTTP 会话
     * @param email     邮箱地址
     * @param password  新密码
     * @param checkCode 验证码
     * @param emailCode 邮箱验证码
     * @return 响应对象
     */
    @RequestMapping("/resetPwd")
    @GlobalInterceptor(checkLogin = false, checkParams = true)
    @Operation(summary = "Reset Password", description = "Reset user password")
    public ResponseVO<Void> resetPwd(HttpSession session,
            @VerifyParam(required = true, regex = VerifyRegexEnum.EMAIL, max = 150) String email,
            @VerifyParam(required = true, regex = VerifyRegexEnum.PASSWORD, min = 8, max = 32) String password,
            @VerifyParam(required = true) String checkCode,
            @VerifyParam(required = true) String emailCode) {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))) {
                throw new BusinessException("图片验证码不正确");
            }
            userInfoService.resetPwd(email, password, emailCode);
            return getSuccessResponseVO(null);
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }

    /**
     * 获取用户头像.
     *
     * @param response HTTP 响应
     * @param userId   用户ID
     */
    @RequestMapping("/getAvatar/{userId}")
    @GlobalInterceptor(checkLogin = false, checkParams = true)
    @Operation(summary = "Get Avatar", description = "Get user avatar image")
    public void getAvatar(HttpServletResponse response,
            @VerifyParam(required = true) @PathVariable("userId") String userId) {
        String avatarFolderName = Constants.FILE_FOLDER_AVATAR_NAME;
        String avatarRootPath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + avatarFolderName;
        File folder = new File(avatarRootPath);
        if (!folder.exists() && !folder.mkdirs()) {
            logger.error("Failed to create avatar folder: {}", folder.getAbsolutePath());
        }

        String avatarPath = avatarFolderName + userId + Constants.AVATAR_SUFFIX;
        File file = new File(avatarRootPath + userId + Constants.AVATAR_SUFFIX);
        if (!file.exists()) {
            if (!new File(avatarRootPath + Constants.AVATAR_DEFUALT).exists()) {
                printNoDefaultImage(response);
                return;
            }
            avatarPath = avatarFolderName + Constants.AVATAR_DEFUALT;
        }
        response.setContentType("image/jpg");
        readFile(response, avatarPath);
    }

    private void printNoDefaultImage(HttpServletResponse response) {
        response.setHeader(CONTENT_TYPE, CONTENT_TYPE_VALUE);
        response.setStatus(HttpStatus.OK.value());
        try (PrintWriter writer = response.getWriter()) {
            writer.print("请在头像目录下放置默认头像default_avatar.jpg");
        } catch (Exception e) {
            logger.error("输出无默认图失败", e);
        }
    }

    /**
     * 获取用户信息.
     *
     * @param session HTTP 会话
     * @return 响应对象
     */
    @RequestMapping("/getUserInfo")
    @GlobalInterceptor
    @Operation(summary = "Get User Info", description = "Get current user information")
    public ResponseVO<SessionWebUserDto> getUserInfo(HttpSession session) {
        SessionWebUserDto sessionWebUserDto = getUserInfoFromSession(session);
        return getSuccessResponseVO(sessionWebUserDto);
    }

    /**
     * 获取用户空间使用情况.
     *
     * @param session HTTP 会话
     * @return 响应对象
     */
    @RequestMapping("/getUseSpace")
    @GlobalInterceptor
    @Operation(summary = "Get User Space", description = "Get user storage space usage")
    public ResponseVO<UserSpaceDto> getUseSpace(HttpSession session) {
        SessionWebUserDto sessionWebUserDto = getUserInfoFromSession(session);
        return getSuccessResponseVO(redisComponent.getUserSpaceUse(sessionWebUserDto.getUserId()));
    }

    /**
     * 用户登出.
     *
     * @param session HTTP 会话
     * @param request HTTP 请求
     * @return 响应对象
     */
    @RequestMapping("/logout")
    @Operation(summary = "Logout", description = "User logout")
    public ResponseVO<Void> logout(HttpSession session, HttpServletRequest request) {
        SessionWebUserDto userDto = getUserInfoFromSession(session);
        if (userDto != null) {
            redisComponent.deleteRefreshToken(userDto.getUserId());
        }

        session.invalidate();
        String token = getJwtFromRequest(request);
        if (token != null) {
            long remainingTime = jwtTokenProvider.getRemainingTime(token);
            if (remainingTime > 0) {
                jwtBlacklistService.addToBlacklist(token, remainingTime);
            }
        }
        return getSuccessResponseVO(null);
    }

    /**
     * 刷新令牌.
     *
     * @param refreshToken 刷新令牌
     * @return 响应对象
     */
    @RequestMapping("/refreshToken")
    @GlobalInterceptor(checkLogin = false)
    @Operation(summary = "Refresh Token", description = "Refresh access token using refresh token")
    public ResponseVO<Map<String, Object>> refreshToken(@VerifyParam(required = true) String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }

        String userId = jwtTokenProvider.getUserIdFromJWT(refreshToken);

        if (!redisComponent.validateRefreshToken(userId, refreshToken)) {
            redisComponent.deleteRefreshToken(userId);
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }

        String newAccessToken = jwtTokenProvider.generateToken(userId, null);

        Map<String, Object> result = new HashMap<>();
        result.put("token", newAccessToken);
        result.put("refreshToken", refreshToken);

        return getSuccessResponseVO(result);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringTools.isEmpty(bearerToken)) {
            return null;
        }
        if (bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return bearerToken;
    }

    /**
     * 更新用户头像.
     *
     * @param session HTTP 会话
     * @param avatar  头像文件
     * @return 响应对象
     */
    @RequestMapping("/updateUserAvatar")
    @GlobalInterceptor
    @Operation(summary = "Update Avatar", description = "Update user avatar")
    public ResponseVO<Void> updateUserAvatar(HttpSession session, MultipartFile avatar) {
        if (avatar == null || avatar.isEmpty()) {
            throw new BusinessException(ResponseCodeEnum.CODE_600.getCode(), "请选择头像文件");
        }
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        String baseFolder = appConfig.getProjectFolder();
        File targetFileFolder = new File(baseFolder + Constants.FILE_FOLDER_AVATAR_NAME);
        if (!targetFileFolder.exists() && !targetFileFolder.mkdirs()) {
            logger.error("Failed to create avatar folder: {}", targetFileFolder.getAbsolutePath());
        }
        File targetFile = new File(targetFileFolder.getPath() + "/" + webUserDto.getUserId() + Constants.AVATAR_SUFFIX);
        // basic validation for avatar upload
        if (avatar == null || avatar.isEmpty()) {
            throw new BusinessException(ResponseCodeEnum.CODE_600.getCode(), "请选择头像文件");
        }
        String contentType = avatar.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new BusinessException(ResponseCodeEnum.CODE_600.getCode(), "仅支持 jpg、jpeg、png、gif、bmp 等图片格式");
        }
        long maxSize = 5 * 1024 * 1024; // 5MB
        if (avatar.getSize() > maxSize) {
            throw new BusinessException(ResponseCodeEnum.CODE_600.getCode(), "头像文件不能超过 5MB");
        }
        logger.info("Saving avatar for userId: {}", webUserDto.getUserId());
        try {
            avatar.transferTo(targetFile);
        } catch (Exception e) {
            logger.error("上传头像失败", e);
            throw new BusinessException(ResponseCodeEnum.CODE_500.getCode(), "上传头像失败");
        }

        UserInfo userInfo = new UserInfo();
        userInfo.setQqAvatar("");
        userInfoService.updateUserInfoByUserId(userInfo, webUserDto.getUserId());
        // 更新前端可用的头像链接，使用带时间戳的 URL 以防缓存，确保头像更新能即时显示
        String avatarUrl = "/api/getAvatar/" + webUserDto.getUserId() + "?v=" + System.currentTimeMillis();
        webUserDto.setAvatar(avatarUrl);
        session.setAttribute(Constants.SESSION_KEY, webUserDto);
        return getSuccessResponseVO(null);
    }

    /**
     * 更新密码.
     *
     * @param session  HTTP 会话
     * @param password 新密码
     * @return 响应对象
     */
    @RequestMapping("/updatePassword")
    @GlobalInterceptor(checkParams = true)
    @Operation(summary = "Update Password", description = "Change user password")
    public ResponseVO<Void> updatePassword(HttpSession session,
            @VerifyParam(required = true, regex = VerifyRegexEnum.PASSWORD, min = 8, max = 32) String password) {
        SessionWebUserDto sessionWebUserDto = getUserInfoFromSession(session);
        UserInfo userInfo = new UserInfo();
        userInfo.setPassword(StringTools.encodeByMD5(password));
        userInfoService.updateUserInfoByUserId(userInfo, sessionWebUserDto.getUserId());
        return getSuccessResponseVO(null);
    }

    /**
     * QQ登录.
     *
     * @param session     HTTP 会话
     * @param callbackUrl 回调URL
     * @return 响应对象
     * @throws UnsupportedEncodingException 编码异常
     */
    @RequestMapping("qqlogin")
    @GlobalInterceptor(checkLogin = false, checkParams = true)
    @Operation(summary = "QQ Login", description = "Get QQ login URL")
    public ResponseVO<String> qqlogin(HttpSession session, String callbackUrl) throws UnsupportedEncodingException {
        String state = StringTools.getRandomString(Constants.LENGTH_30);
        if (!StringTools.isEmpty(callbackUrl)) {
            session.setAttribute(state, callbackUrl);
        }
        String url = String.format(appConfig.getQqUrlAuthorization(), appConfig.getQqAppId(),
                URLEncoder.encode(appConfig.getQqUrlRedirect(), "utf-8"), state);
        return getSuccessResponseVO(url);
    }

    /**
     * QQ登录回调.
     *
     * @param session HTTP 会话
     * @param code    授权码
     * @param state   状态参数
     * @return 响应对象
     */
    @RequestMapping("qqlogin/callback")
    @GlobalInterceptor(checkLogin = false, checkParams = true)
    @Operation(summary = "QQ Login Callback", description = "Callback for QQ login")
    public ResponseVO<Map<String, Object>> qqLoginCallback(HttpSession session,
            @VerifyParam(required = true) String code,
            @VerifyParam(required = true) String state) {
        SessionWebUserDto sessionWebUserDto = userInfoService.qqLogin(code);
        session.setAttribute(Constants.SESSION_KEY, sessionWebUserDto);
        Map<String, Object> result = new HashMap<>();
        result.put("callbackUrl", session.getAttribute(state));
        result.put("userInfo", sessionWebUserDto);
        return getSuccessResponseVO(result);
    }
}
