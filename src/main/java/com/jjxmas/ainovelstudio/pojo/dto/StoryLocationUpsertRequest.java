package com.jjxmas.ainovelstudio.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StoryLocationUpsertRequest {

    @NotBlank(message = "地点名称不能为空")
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
