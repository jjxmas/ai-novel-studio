package com.jjxmas.ainovelstudio.pojo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OutlineWorkflowCreateRequest {

    @NotNull(message = "作品 ID 不能为空")
    private Long projectId;

    private Long modelConfigId;
}
