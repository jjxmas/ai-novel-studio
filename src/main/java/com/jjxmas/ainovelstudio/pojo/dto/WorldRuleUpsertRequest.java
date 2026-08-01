package com.jjxmas.ainovelstudio.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WorldRuleUpsertRequest {

    @NotBlank(message = "规则名称不能为空")
    private String name;

    private String ruleType;

    @NotBlank(message = "规则内容不能为空")
    private String description;

    private String triggerCondition;

    private String effectResult;

    private String limitations;

    private String cost;

    private String exceptions;

    private String visibilityLevel;

    private Integer importance;

    private String examples;

    private String notes;
}
