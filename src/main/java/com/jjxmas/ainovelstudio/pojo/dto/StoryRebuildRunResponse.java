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
public class StoryRebuildRunResponse {

    private Long runId;
    private Long generationJobId;
    private Long projectId;
    private String status;
    private String phase;
    private Integer requestedStartChapterNo;
    private Integer actualStartChapterNo;
    private Integer nextFactChapterNo;
    private Integer nextMemoryChapterNo;
    private Integer processedChapterCount;
    private Integer skippedChapterCount;
    private String errorMessage;
    private StoryRebuildResult result;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
