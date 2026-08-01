package com.jjxmas.ainovelstudio.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jjxmas.ainovelstudio.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("locations")
public class StoryLocation extends BaseEntity {

    private Long projectId;

    private String name;

    private String locationType;

    private String description;

    private String keyFeatures;

    private String entryConditions;

    private String availableResources;

    private Long controllingOrgId;

    private String riskLevel;

    private String rules;

    private Long parentLocationId;

    private String notes;
}
