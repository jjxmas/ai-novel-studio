package com.jjxmas.ainovelstudio.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StoryItemUpsertRequest {

    @NotBlank(message = "物品名称不能为空")
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
