package com.easypan.config;

import com.easypan.component.JwtAuthenticationFilter;
import com.easypan.service.CustomOAuth2UserService;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * OAuth2 安全配置类
 */
@Configuration
@EnableWebSecurity
public class OAuth2SecurityConfig {

    @Resource
    private CustomOAuth2UserService customOAuth2UserService;

    @Resource
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用 CSRF (因为主要是 API 服务，且已有 token/session 机制)
            .csrf(csrf -> csrf.disable())
            
            // 允许所有请求通过 Spring Security (由 GlobalInterceptor 处理业务权限)
            // 仅对 OAuth2 登录路径进行拦截处理
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            )
            
            // 添加 JWT 过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            
            // 配置 OAuth2 登录
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
            );
            
        return http.build();
    }
}
