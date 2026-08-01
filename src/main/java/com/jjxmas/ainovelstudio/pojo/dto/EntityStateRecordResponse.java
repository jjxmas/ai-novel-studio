package com.jjxmas.ainovelstudio.pojo.dto;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityStateRecordResponse {

    private Long id;

    private Long projectId;

    private String entityType;

    private Long entityId;

    private String stateType;

    private Map<String, Object> oldValue;

    private Map<String, Object> newValue;

    private Long eventId;

    private Long chapterId;

    private LocalDateTime effectiveAt;
}
