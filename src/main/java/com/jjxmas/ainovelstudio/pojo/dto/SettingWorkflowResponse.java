package com.jjxmas.ainovelstudio.pojo.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SettingWorkflowResponse {

    private Long id;

    private Long projectId;

    private Long sourceIdeaId;

    private String status;

    private Object blueprint;

    private Object draft;

    private Object checks;

    private LocalDateTime blueprintConfirmedAt;

    private LocalDateTime committedAt;
}
