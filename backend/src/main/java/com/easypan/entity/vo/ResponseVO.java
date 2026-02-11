package com.easypan.entity.vo;

import lombok.Data;

@Data
public class ResponseVO<T> {
    private String status;
    private Integer code;
    private String info;
    private T data;
}
