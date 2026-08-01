package com.jjxmas.ainovelstudio.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jjxmas.ainovelstudio.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("world_rules")
public class WorldRule extends BaseEntity {

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
