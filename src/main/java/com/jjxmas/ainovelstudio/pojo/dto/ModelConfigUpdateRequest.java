package com.jjxmas.ainovelstudio.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ModelConfigUpdateRequest {

    @NotBlank(message = "模型供应商不能为空")
    private String provider;

    @NotBlank(message = "显示名称不能为空")
    private String displayName;

    private String baseUrl;

    @NotBlank(message = "模型名称不能为空")
    private String modelName;

    private String apiKey;

    @NotBlank(message = "模型用途不能为空")
    private String usageType;

    private Boolean defaultModel;

    private Boolean enabled;
}
