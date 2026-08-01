package com.jjxmas.ainovelstudio.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrganizationUpsertRequest {

    @NotBlank(message = "组织名称不能为空")
    private String name;

    private String organizationType;

    private String publicMission;

    private String realGoal;

    private String controlledResources;

    private String powerScope;

    private Long baseLocationId;

    private String entryRules;

    private String status;

    private String notes;
}
