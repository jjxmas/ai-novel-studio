package com.jjxmas.ainovelstudio.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorldRuleResponse {

    private Long id;

    private Long projectId;

    private String name;

    private String ruleType;

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
