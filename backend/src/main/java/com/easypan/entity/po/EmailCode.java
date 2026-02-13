package com.easypan.entity.po;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 邮箱验证码实体类.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("email_code")
public class EmailCode implements Serializable {

    /**
     * 邮箱.
     */
    @Id(keyType = KeyType.None)
    private String email;

    /**
     * 编号.
     */
    private String code;

    /**
     * 创建时间.
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /**
     * 0:未使用 1:已使用.
     */
    private Integer status;
}
