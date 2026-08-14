package com.jjxmas.ainovelstudio.pojo.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChapterGenerationBatchResponse {

    private Long batchId;
    private Long projectId;
    private String batchType;
    private Long modelConfigId;
    private String status;
    private Integer totalCount;
    private Integer pendingCount;
    private Integer runningCount;
    private Integer succeededCount;
    private Integer failedCount;
    private Integer skippedCount;
    private Integer qualityCheckedCount;
    private Integer qualityFailedCount;
    private Integer qualityIssueCount;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private List<ChapterGenerationBatchItemResponse> items;
}
