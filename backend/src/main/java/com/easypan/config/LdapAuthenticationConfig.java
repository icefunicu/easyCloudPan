package com.easypan.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * LDAP 认证配置.
 * 仅在配置 ldap.enabled=true 时生效.
 */
@Configuration
@ConditionalOnProperty(name = "ldap.enabled", havingValue = "true")
public class LdapAuthenticationConfig {

    @Value("${ldap.urls:ldap://localhost:8389/}")
    private String ldapUrls;

    @Value("${ldap.base-dn:dc=springframework,dc=org}")
    private String ldapBaseDn;

    @Value("${ldap.user-dn-pattern:uid={0},ou=people}")
    private String userDnPattern;

    /**
     * 配置 LDAP 认证.
     *
     * @param auth 认证管理器构建器
     * @param passwordEncoder 密码编码器
     * @throws Exception 配置异常
     */
    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth, PasswordEncoder passwordEncoder) throws Exception {
        auth.ldapAuthentication()
                .userDnPatterns(userDnPattern)
                .contextSource()
                .url(ldapUrls + ldapBaseDn)
                .and()
                .passwordCompare()
                .passwordEncoder(passwordEncoder)
                .passwordAttribute("userPassword");
    }
}
