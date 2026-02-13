package com.easypan.annotation;

import org.springframework.web.bind.annotation.Mapping;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 全局拦截器注解.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Mapping
public @interface GlobalInterceptor {

    /**
     * 校验登录.
     *
     * @return 是否校验登录
     */
    boolean checkLogin() default true;

    /**
     * 校验参数.
     *
     * @return 是否校验参数
     */
    boolean checkParams() default false;

    /**
     * 校验管理员.
     *
     * @return 是否校验管理员
     */
    boolean checkAdmin() default false;
}
