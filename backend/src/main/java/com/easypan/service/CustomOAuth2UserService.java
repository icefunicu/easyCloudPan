package com.easypan.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/**
 * 自定义 OAuth2 用户服务
 * 用于处理第三方登录成功后的用户信息获取和本地用户映射
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger logger = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        logger.info("Loading user from provider: {}", userRequest.getClientRegistration().getRegistrationId());
        OAuth2User oAuth2User = super.loadUser(userRequest);
        
        // TODO: 这里可以添加逻辑将第三方用户保存到本地数据库
        // String registrationId = userRequest.getClientRegistration().getRegistrationId();
        // Map<String, Object> attributes = oAuth2User.getAttributes();
        
        return oAuth2User;
    }
}
