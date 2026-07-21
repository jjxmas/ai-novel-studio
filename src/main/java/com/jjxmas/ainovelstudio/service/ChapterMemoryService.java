package com.jjxmas.ainovelstudio.service;

import com.jjxmas.ainovelstudio.pojo.entity.Chapter;
import com.jjxmas.ainovelstudio.pojo.dto.ProjectMemoryResponse;
import java.util.Map;

/**
 * 章节记忆服务，负责构建生成上下文和维护项目记忆。
 */
public interface ChapterMemoryService {

    /**
     * 为章节生成构建包含项目、大纲、设定和记忆的上下文。
     */
    Map<String, Object> buildChapterContext(Chapter chapter);

    /**
     * 在章节正文变化后刷新摘要和分层记忆。
     */
    void refreshAfterChapterContent(Chapter chapter, Long modelConfigId);

    /**
     * 查询指定项目的完整记忆视图。
     */
    ProjectMemoryResponse getProjectMemory(Long projectId);
}
