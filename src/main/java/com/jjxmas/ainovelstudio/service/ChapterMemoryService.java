package com.jjxmas.ainovelstudio.service;

import com.jjxmas.ainovelstudio.pojo.dto.ProjectMemoryResponse;
import com.jjxmas.ainovelstudio.pojo.entity.Chapter;

public interface ChapterMemoryService {

    void refreshAfterChapterContent(Chapter chapter, Long modelConfigId);

    void refreshFactProjection(Chapter chapter, Long modelConfigId);

    void clearFactProjection(Chapter chapter);

    void refreshNarrativeMemory(Chapter chapter, Long modelConfigId);

    void resetNarrativeMemory(Long projectId);

    ProjectMemoryResponse getProjectMemory(Long projectId);
}
