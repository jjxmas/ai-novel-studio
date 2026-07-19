package com.jjxmas.ainovelstudio.module.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 新增模型配置请求。第一阶段不测试真实连接。
 */
@Data
public class ModelConfigCreateRequest {

    @NotBlank(message = "模型供应商不能为空")
    private String provider;

    @NotBlank(message = "显示名称不能为空")
    private String displayName;

    private String baseUrl;

    @NotBlank(message = "模型名称不能为空")
    private String modelName;

    @NotBlank(message = "API Key 不能为空")
    private String apiKey;

    @NotBlank(message = "模型用途不能为空")
    private String usageType;

    private Boolean defaultModel;

    private Boolean enabled;
}
