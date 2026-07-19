package com.jjxmas.ainovelstudio.module.idea.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class IdeaRewriteRequest {

    private Long modelConfigId;

    @NotBlank(message = "修改意见不能为空")
    private String instruction;
}

