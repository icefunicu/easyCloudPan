package com.easypan.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * 国际化配置类
 * 支持中英文错误消息
 */
@Configuration
public class I18nConfig implements WebMvcConfigurer {

    /**
     * 配置 MessageSource Bean
     * 用于加载国际化资源文件
     */
    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        
        // 设置资源文件基础名称（不包含语言和扩展名）
        messageSource.setBasename("messages");
        
        // 设置编码格式为 UTF-8
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        
        // 设置默认语言为中文
        messageSource.setDefaultLocale(Locale.SIMPLIFIED_CHINESE);
        
        // 设置缓存时间（秒），-1 表示永久缓存，开发环境可设置为 0
        messageSource.setCacheSeconds(3600);
        
        // 找不到消息时使用消息代码作为默认值
        messageSource.setUseCodeAsDefaultMessage(true);
        
        return messageSource;
    }

    /**
     * 配置 LocaleResolver
     * 用于解析用户的语言偏好
     */
    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver localeResolver = new SessionLocaleResolver();
        
        // 设置默认语言为中文
        localeResolver.setDefaultLocale(Locale.SIMPLIFIED_CHINESE);
        
        return localeResolver;
    }

    /**
     * 配置 LocaleChangeInterceptor
     * 允许通过请求参数切换语言
     */
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        
        // 设置请求参数名称，例如: ?lang=en_US
        interceptor.setParamName("lang");
        
        return interceptor;
    }

    /**
     * 注册拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }
}
