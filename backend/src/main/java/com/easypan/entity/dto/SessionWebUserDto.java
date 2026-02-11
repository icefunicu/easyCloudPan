package com.easypan.entity.dto;

import lombok.Data;

@Data
public class SessionWebUserDto {
    private String nickName;
    private String userId;
    /**
     * 将字段名从 isAdmin 改为 admin，使 Lombok 生成 getAdmin 和 setAdmin 以保持兼容性
     */
    private Boolean admin;
    private String avatar;
}
