package com.jjxmas.ainovelstudio.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryLocationResponse {

    private Long id;

    private Long projectId;

    private String name;

    private String locationType;

    private Long parentLocationId;

    private String description;

    private String keyFeatures;

    private String entryConditions;

    private String availableResources;

    private Long controllingOrgId;

    private String riskLevel;

    private String rules;

    private String notes;
}
