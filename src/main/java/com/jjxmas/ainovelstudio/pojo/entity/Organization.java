package com.jjxmas.ainovelstudio.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jjxmas.ainovelstudio.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("organizations")
public class Organization extends BaseEntity {

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
