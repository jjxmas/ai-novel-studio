package com.jjxmas.ainovelstudio.module.setting.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 设定库生成请求" */
@Data
public class SettingLibraryGenerateRequest {

    @NotNull(message = "作品 ID 不能为空")
    private Long projectId;

    private Long ideaId;

    private String sourceIdeaSummary;

    private Long modelConfigId;
}

