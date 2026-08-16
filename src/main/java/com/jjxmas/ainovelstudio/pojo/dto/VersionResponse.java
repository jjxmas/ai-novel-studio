package com.jjxmas.ainovelstudio.pojo.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionResponse {

    private Long id;

    private Long projectId;

    private String entityType;

    private Long entityId;

    private Integer versionNo;

    private String changeSource;

    private String operationType;

    private String changeNote;

    private String revisionInstruction;

    private Long modelConfigId;

    private Long jobId;

    private String snapshot;

    private LocalDateTime createdAt;
}
