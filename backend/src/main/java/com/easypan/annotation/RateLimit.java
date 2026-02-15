package com.easypan.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 限流key，唯一标识.
     */
    String key() default "";

    /**
     * 限流时间，单位秒.
     */
    int time() default 60;

    /**
     * 限流次数.
     */
    int count() default 10;
}
