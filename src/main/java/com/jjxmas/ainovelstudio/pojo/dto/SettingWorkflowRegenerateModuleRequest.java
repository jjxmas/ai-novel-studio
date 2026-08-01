package com.jjxmas.ainovelstudio.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SettingWorkflowRegenerateModuleRequest {

    @NotBlank(message = "模块不能为空")
    private String moduleKey;
}
