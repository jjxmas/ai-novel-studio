package com.jjxmas.ainovelstudio.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationResponse {

    private Long id;

    private Long projectId;

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
