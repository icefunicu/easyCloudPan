package com.easypan.exception;

import com.easypan.entity.enums.ResponseCodeEnum;

/**
 * 业务异常类，用于封装业务逻辑中的异常情况.
 */
public class BusinessException extends RuntimeException {

    private ResponseCodeEnum codeEnum;

    private Integer code;

    private String message;

    /**
     * 构造函数，包含消息和原因.
     *
     * @param message 异常消息
     * @param e 异常原因
     */
    public BusinessException(String message, Throwable e) {
        super(message, e);
        this.message = message;
    }

    public BusinessException(String message) {
        super(message);
        this.message = message;
    }

    public BusinessException(Throwable e) {
        super(e);
    }

    /**
     * 构造函数，使用响应码枚举.
     *
     * @param codeEnum 响应码枚举
     */
    public BusinessException(ResponseCodeEnum codeEnum) {
        super(codeEnum.getMsg());
        this.codeEnum = codeEnum;
        this.code = codeEnum.getCode();
        this.message = codeEnum.getMsg();
    }

    /**
     * 构造函数，使用自定义错误码和消息.
     *
     * @param code 错误码
     * @param message 异常消息
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public ResponseCodeEnum getCodeEnum() {
        return codeEnum;
    }

    public Integer getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    /**
     * 重写fillInStackTrace，业务异常不需要堆栈信息，提高效率.
     */
    @Override
    public Throwable fillInStackTrace() {
        return this;
    }

}
