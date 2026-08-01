package com.jjxmas.ainovelstudio.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityRelationResponse {

    private Long id;

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
