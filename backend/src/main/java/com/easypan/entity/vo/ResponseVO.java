package com.easypan.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 响应视图对象.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResponseVO<T> {
    private String status;
    private Integer code;
    private String info;
    private String suggestion;
    private T data;
}
