package com.jjxmas.ainovelstudio.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jjxmas.ainovelstudio.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("items")
public class StoryItem extends BaseEntity {

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
