package com.jjxmas.ainovelstudio.pojo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 大纲生成请求" */
@Data
public class OutlineGenerateRequest {

    @NotNull(message = "作品 ID 不能为空")
    private Long projectId;

    private String outlineLevel;

    private String sourceContent;

    private Long modelConfigId;
}

