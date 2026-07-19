package com.jjxmas.ainovelstudio.module.memory.service;

import com.jjxmas.ainovelstudio.module.chapter.entity.Chapter;
import com.jjxmas.ainovelstudio.module.memory.dto.ProjectMemoryResponse;
import java.util.Map;

public interface ChapterMemoryService {

    Map<String, Object> buildChapterContext(Chapter chapter);

    void refreshAfterChapterContent(Chapter chapter, Long modelConfigId);

    ProjectMemoryResponse getProjectMemory(Long projectId);
}
