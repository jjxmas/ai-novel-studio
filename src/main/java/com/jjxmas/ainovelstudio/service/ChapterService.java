package com.jjxmas.ainovelstudio.service;

import com.jjxmas.ainovelstudio.pojo.dto.ChapterContentUpdateRequest;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterGenerateRequest;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterResponse;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterRewriteRequest;
import java.util.List;

/**
 * 章节服务，提供章节大纲确认、正文生成、编辑和重写能力。
 */
public interface ChapterService {

    /**
     * 查询指定项目下的章节列表。
     */
    List<ChapterResponse> listChapters(Long projectId);

    /**
     * 确认指定章节的大纲。
     */
    ChapterResponse confirmChapterOutline(Long chapterId);

    /**
     * 根据章节生成请求生成正文。
     */
    ChapterResponse generateChapter(ChapterGenerateRequest request);

    /**
     * 更新指定章节的正文内容。
     */
    ChapterResponse updateChapterContent(Long chapterId, ChapterContentUpdateRequest request);

    /**
     * 根据重写指令重新生成章节正文。
     */
    ChapterResponse rewriteChapter(Long chapterId, ChapterRewriteRequest request);
}
