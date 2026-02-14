package com.easypan.service.oauth;

import com.easypan.component.RedisComponent;
import com.easypan.entity.config.AppConfig;
import com.easypan.entity.constants.Constants;
import com.easypan.entity.dto.SessionWebUserDto;
import com.easypan.entity.dto.UserSpaceDto;
import com.easypan.entity.enums.UserStatusEnum;
import com.easypan.entity.po.UserInfo;
import com.easypan.exception.BusinessException;
import com.easypan.mappers.UserInfoMapper;
import com.easypan.service.FileInfoService;
import com.easypan.utils.StringTools;
import com.mybatisflex.core.query.QueryWrapper;
import com.easypan.entity.dto.OAuthCallbackDto;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.easypan.entity.po.table.UserInfoTableDef.USER_INFO;

@Service
public class OAuthLoginService {

    @Resource
    private List<OAuthProviderStrategy> strategyList;

    @Resource
    private UserInfoMapper userInfoMapper;

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private FileInfoService fileInfoService;

    @Resource
    private AppConfig appConfig;

    @Resource
    private PasswordEncoder passwordEncoder;

    private Map<String, OAuthProviderStrategy> strategyMap = new HashMap<>();

    @PostConstruct
    public void init() {
        for (OAuthProviderStrategy strategy : strategyList) {
            strategyMap.put(strategy.getProviderName(), strategy);
        }
    }

    public String buildAuthorizationUrl(String provider, String state) {
        OAuthProviderStrategy strategy = strategyMap.get(provider);
        if (strategy == null) {
            throw new BusinessException("Unsupported OAuth provider: " + provider);
        }
        return strategy.buildAuthorizationUrl(state);
    }

    @Transactional(rollbackFor = Exception.class)
    public OAuthCallbackDto login(String provider, String code) {
        OAuthProviderStrategy strategy = strategyMap.get(provider);
        if (strategy == null) {
            throw new BusinessException("Unsupported OAuth provider: " + provider);
        }

        String accessToken = strategy.getAccessToken(code);
        OAuthUserInfo oauthUser = strategy.getUserInfo(accessToken);
        oauthUser.setProvider(provider); // Ensure provider is set in OAuthUserInfo if possible, or carry it

        // 1. Find by provider + providerId
        UserInfo user = userInfoMapper.selectOneByQuery(QueryWrapper.create()
                .where(USER_INFO.OAUTH_PROVIDER.eq(provider))
                .and(USER_INFO.OAUTH_PROVIDER_ID.eq(oauthUser.getProviderId())));

        // 2. If not found, try to find by email if available to auto-link
        if (user == null && !StringTools.isEmpty(oauthUser.getEmail())) {
            user = userInfoMapper.selectOneByQuery(QueryWrapper.create()
                    .where(USER_INFO.EMAIL.eq(oauthUser.getEmail())));
            // If found by email, we link it
            if (user != null) {
                UserInfo updateInfo = new UserInfo();
                updateInfo.setOauthProvider(provider);
                updateInfo.setOauthProviderId(oauthUser.getProviderId());
                if (StringTools.isEmpty(user.getAvatar())) {
                    updateInfo.setAvatar(oauthUser.getAvatarUrl());
                }
                userInfoMapper.updateByQuery(updateInfo,
                        QueryWrapper.create().where(USER_INFO.USER_ID.eq(user.getUserId())));
            }
        }

        OAuthCallbackDto result = new OAuthCallbackDto();

        // 3. If user exists (either found by providerId or linked by email), login
        if (user != null) {
            checkUserStatus(user);
            SessionWebUserDto sessionWebUserDto = createSessionUser(user);
            result.setLoginSuccess(true);
            result.setSessionWebUserDto(sessionWebUserDto);
        } else {
            // 4. If user not found, return info for registration
            result.setLoginSuccess(false);
            result.setOauthUserInfo(oauthUser);
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public SessionWebUserDto register(String provider, OAuthUserInfo oauthUser, String password) {
        // Double check if user exists (concurrency)
        UserInfo existing = userInfoMapper.selectOneByQuery(QueryWrapper.create()
                .where(USER_INFO.EMAIL.eq(oauthUser.getEmail())));
        if (existing != null) {
            throw new BusinessException("该邮箱已被注册，请直接登录关联");
        }

        UserInfo user = new UserInfo();
        user.setUserId(StringTools.getRandomString(Constants.LENGTH_10));
        user.setNickName(oauthUser.getNickname());
        user.setEmail(oauthUser.getEmail());
        user.setPassword(passwordEncoder.encode(password));
        user.setAvatar(oauthUser.getAvatarUrl());
        user.setOauthProvider(provider);
        user.setOauthProviderId(oauthUser.getProviderId());
        user.setJoinTime(new Date());
        user.setLastLoginTime(new Date());
        user.setStatus(UserStatusEnum.ENABLE.getStatus());
        user.setUseSpace(0L);
        user.setTotalSpace(redisComponent.getSysSettingsDto().getUserInitUseSpace() * Constants.MB);

        // Handle duplicate nickname
        UserInfo existingNick = userInfoMapper.selectOneByQuery(
                QueryWrapper.create().where(USER_INFO.NICK_NAME.eq(user.getNickName())));
        if (existingNick != null) {
            user.setNickName(user.getNickName() + "_" + StringTools.getRandomString(5));
        }

        userInfoMapper.insert(user);
        return createSessionUser(user);
    }

    private void checkUserStatus(UserInfo user) {
        if (UserStatusEnum.DISABLE.getStatus().equals(user.getStatus())) {
            throw new BusinessException("账号被禁用无法登录");
        }
    }

    private SessionWebUserDto createSessionUser(UserInfo user) {
        SessionWebUserDto sessionWebUserDto = new SessionWebUserDto();
        sessionWebUserDto.setUserId(user.getUserId());
        sessionWebUserDto.setNickName(user.getNickName());
        sessionWebUserDto.setAvatar(user.getAvatar());
        if ((user.getIsAdmin() != null && user.getIsAdmin())
                || ArrayUtils.contains(appConfig.getAdminEmails().split(","),
                        user.getEmail() == null ? "" : user.getEmail())) {
            sessionWebUserDto.setAdmin(true);
        } else {
            sessionWebUserDto.setAdmin(false);
        }

        UserSpaceDto userSpaceDto = new UserSpaceDto();
        userSpaceDto.setUseSpace(fileInfoService.getUserUseSpace(user.getUserId()));
        userSpaceDto.setTotalSpace(user.getTotalSpace());
        redisComponent.saveUserSpaceUse(user.getUserId(), userSpaceDto);
        return sessionWebUserDto;
    }
}
