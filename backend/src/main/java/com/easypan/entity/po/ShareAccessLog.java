package com.easypan.entity.po;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.mybatisflex.annotation.Table;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("share_access_log")
public class ShareAccessLog implements Serializable {

    private Long id;

    private String shareId;

    private String fileId;

    private String visitorId;

    private String visitorIp;

    private String visitorUserAgent;

    private String accessType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date accessTime;

    private Boolean success;

    private String errorMessage;
}
