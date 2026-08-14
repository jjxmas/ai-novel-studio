package com.jjxmas.ainovelstudio.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExportRequest {

    @NotNull(message = "PROJECT_ID_REQUIRED")
    private Long projectId;

    @NotBlank(message = "SCOPE_REQUIRED")
    private String scope;

    @NotBlank(message = "FORMAT_REQUIRED")
    private String format;

    private Long scopeEntityId;
}