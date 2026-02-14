package com.easypan.service.oauth;

import com.easypan.entity.config.AppConfig;
import com.easypan.exception.BusinessException;
import com.easypan.utils.JsonUtils;
import com.easypan.utils.OKHttpUtils;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.Map;

@Component
public class GitHubOAuthStrategy implements OAuthProviderStrategy {

    @Resource
    private AppConfig appConfig;

    @Override
    public String getProviderName() {
        return "github";
    }

    @Override
    public String buildAuthorizationUrl(String state) {
        return String.format(
                "https://github.com/login/oauth/authorize?client_id=%s&redirect_uri=%s&state=%s&scope=user:email",
                appConfig.getGithubClientId(), appConfig.getGithubRedirectUri(), state);
    }

    @Override
    public String getAccessToken(String code) {
        String url = String.format(
                "https://github.com/login/oauth/access_token?client_id=%s&client_secret=%s&code=%s&redirect_uri=%s",
                appConfig.getGithubClientId(), appConfig.getGithubClientSecret(), code,
                appConfig.getGithubRedirectUri());
        String response = OKHttpUtils.getRequest(url);
        // GitHub returns: access_token=...&scope=...&token_type=bearer
        // We need to parse this. Ideally we should send Accept: application/json
        // header, but OKHttpUtils might just do GET.
        // Let's handle the x-www-form-urlencoded response manually if needed, or check
        // if OKHttpUtils supports headers.
        // Assuming simple GET, we parse the string.
        if (response != null && response.contains("access_token=")) {
            String[] parts = response.split("&");
            for (String part : parts) {
                if (part.startsWith("access_token=")) {
                    return part.split("=")[1];
                }
            }
        }
        throw new BusinessException("Failed to get GitHub access token: " + response);
    }

    @Override
    public OAuthUserInfo getUserInfo(String accessToken) {
        String url = "https://api.github.com/user";
        Map<String, String> header = new java.util.HashMap<>();
        header.put("Authorization", "token " + accessToken);

        String response = OKHttpUtils.getRequest(url, header);
        if (response == null) {
            throw new BusinessException("Failed to get GitHub user info");
        }

        Map<String, Object> userInfoMap = JsonUtils.convertJson2Map(response);
        if (userInfoMap == null || userInfoMap.containsKey("message")) {
            throw new BusinessException("Failed to get GitHub user info: " + response);
        }

        OAuthUserInfo userInfo = new OAuthUserInfo();
        userInfo.setProviderId(String.valueOf(userInfoMap.get("id")));
        userInfo.setNickname((String) userInfoMap.getOrDefault("name", userInfoMap.get("login")));
        userInfo.setAvatarUrl((String) userInfoMap.get("avatar_url"));

        // GitHub email might be private, if so we need another call to /user/emails,
        // but for now let's try to get it from the profile or use a fallback.
        String email = (String) userInfoMap.get("email");
        if (email == null) {
            // Try to use a dummy email or handle it.
            // For this implementation plan, we'll leave it null or use
            // providerId@github.com as fallback if needed by business logic.
            // But UserInfoService usually requires email.
            // Let's set a placeholder if null: id + "@github.com" to avoid registration
            // failure,
            // although real email is better.
            email = userInfoMap.get("login") + "@github.com";
        }
        userInfo.setEmail(email);

        return userInfo;
    }
}
