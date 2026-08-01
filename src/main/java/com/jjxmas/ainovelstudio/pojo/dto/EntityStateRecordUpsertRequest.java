package com.jjxmas.ainovelstudio.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.Data;

@Data
public class EntityStateRecordUpsertRequest {

    @NotBlank(message = "实体类型不能为空")
    private String entityType;

    @NotNull(message = "实体ID不能为空")
    private Long entityId;

    @NotBlank(message = "状态类型不能为空")
    private String stateType;

    private Map<String, Object> oldValue;

    @NotNull(message = "新状态不能为空")
    private Map<String, Object> newValue;

    private Long eventId;

    private Long chapterId;

    private LocalDateTime effectiveAt;
}
