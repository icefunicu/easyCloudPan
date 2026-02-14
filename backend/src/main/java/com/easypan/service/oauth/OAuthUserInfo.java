package com.easypan.service.oauth;

import lombok.Data;

@Data
public class OAuthUserInfo {
    private String providerId; // Provider 唯一用户 ID
    private String nickname;
    private String email;
    private String avatarUrl;
    private String provider;
}
