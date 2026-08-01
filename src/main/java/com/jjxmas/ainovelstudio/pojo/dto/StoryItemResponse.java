package com.jjxmas.ainovelstudio.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryItemResponse {

    private Long id;

    private Long projectId;

    private String name;

    private String itemType;

    private String description;

    private String usageRules;

    private String limitations;

    private String rarity;

    private Long ownerCharacterId;

    private Long ownerOrgId;

    private String status;

    private String notes;
}
