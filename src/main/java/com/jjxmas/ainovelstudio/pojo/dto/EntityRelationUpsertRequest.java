package com.jjxmas.ainovelstudio.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EntityRelationUpsertRequest {

    @NotBlank(message = "源实体类型不能为空")
    private String sourceType;

    @NotNull(message = "源实体ID不能为空")
    private Long sourceId;

    @NotBlank(message = "目标实体类型不能为空")
    private String targetType;

    @NotNull(message = "目标实体ID不能为空")
    private Long targetId;

    @NotBlank(message = "关系类型不能为空")
    private String relationType;

    private String relationStatus;

    private Integer strengthValue;

    private String visibilityLevel;

    private String note;

    private Long startEventId;

    private Long endEventId;
}
