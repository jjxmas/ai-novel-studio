package com.jjxmas.ainovelstudio.pojo.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OutlineWorkflowResponse {

    private Long id;

    private Long projectId;

    private Long settingLibraryId;

    private String status;

    private Object draft;

    private Object checks;

    private LocalDateTime committedAt;
}
