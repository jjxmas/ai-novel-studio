package com.jjxmas.ainovelstudio.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 导出请求“ */
@Data
public class ExportRequest {

    @NotNull(message = "作品 ID 不能为空")
    private Long projectId;

    @NotBlank(message = "导出范围不能为空")
    private String scope;

    @NotBlank(message = "导出格式不能为空")
    private String format;
}

