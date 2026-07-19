package com.jjxmas.ainovelstudio.module.memory.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectMemoryResponse {

    private Long projectId;

    private StoryMemoryResponse globalMemory;

    private List<StoryMemoryResponse> highMemories;

    private List<StoryMemoryResponse> middleMemories;

    private List<StoryMemoryResponse> recentWindows;

    private List<ChapterSummaryResponse> recentChapterSummaries;
}
