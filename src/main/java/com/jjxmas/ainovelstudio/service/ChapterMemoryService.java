package com.jjxmas.ainovelstudio.service;

import com.jjxmas.ainovelstudio.pojo.dto.ProjectMemoryResponse;
import com.jjxmas.ainovelstudio.pojo.entity.Chapter;

public interface ChapterMemoryService {

    void refreshAfterChapterContent(Chapter chapter, Long modelConfigId);

    ProjectMemoryResponse getProjectMemory(Long projectId);
}
