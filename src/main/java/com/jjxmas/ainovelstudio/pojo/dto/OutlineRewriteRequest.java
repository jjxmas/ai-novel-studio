package com.jjxmas.ainovelstudio.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OutlineRewriteRequest {

    private Long modelConfigId;

    @NotBlank(message = "修改意见不能为空")
    private String instruction;
}

