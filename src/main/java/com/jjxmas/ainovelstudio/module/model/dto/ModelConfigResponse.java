package com.jjxmas.ainovelstudio.module.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelConfigResponse {

    private Long id;

    private String provider;

    private String displayName;

    private String baseUrl;

    private String modelName;

    private String usageType;

    private Boolean hasApiKey;

    private Boolean defaultModel;

    private Boolean enabled;
}

