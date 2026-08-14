package com.jjxmas.ainovelstudio.pojo.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChapterGenerationBatchItemResponse {

    private Long id;
    private Long chapterId;
    private Integer chapterNo;
    private String status;
    private Integer attemptCount;
    private Long generationJobId;
    private String qualityStatus;
    private Integer qualityIssueCount;
    private CheckResponse qualityReport;
    private String qualityErrorMessage;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
