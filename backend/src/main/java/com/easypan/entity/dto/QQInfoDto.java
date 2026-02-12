package com.easypan.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "QQ User Information")
public class QQInfoDto {
    @Schema(description = "Return Code")
    private Integer ret;
    @Schema(description = "Message")
    private String msg;
    @Schema(description = "Nickname")
    private String nickname;
    @Schema(description = "Avatar URL 1")
    private String figureurl_qq_1;
    @Schema(description = "Avatar URL 2")
    private String figureurl_qq_2;
    @Schema(description = "Gender")
    private String gender;
}
