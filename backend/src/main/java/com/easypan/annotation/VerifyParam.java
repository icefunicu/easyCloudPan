package com.easypan.annotation;

import com.easypan.entity.enums.VerifyRegexEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 参数校验注解.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
public @interface VerifyParam {

    /**
     * 校验正则.
     *
     * @return 正则枚举
     */
    VerifyRegexEnum regex() default VerifyRegexEnum.NO;

    /**
     * 最小长度.
     *
     * @return 最小长度值
     */
    int min() default -1;

    /**
     * 最大长度.
     *
     * @return 最大长度值
     */
    int max() default -1;

    /**
     * 是否必填.
     *
     * @return 是否必填
     */
    boolean required() default false;
}
