package com.jjxmas.ainovelstudio.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jjxmas.ainovelstudio.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("entity_relations")
public class EntityRelation extends BaseEntity {

    private Long projectId;

    private String sourceType;

    private Long sourceId;

    private String targetType;

    private Long targetId;

    private String relationType;

    private String relationStatus;

    private Integer strengthValue;

    private String visibilityLevel;

    private String note;

    private Long startEventId;

    private Long endEventId;
}
