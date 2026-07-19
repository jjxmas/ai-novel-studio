package com.jjxmas.ainovelstudio.module.chapter.service;

import com.jjxmas.ainovelstudio.module.chapter.dto.ChapterContentUpdateRequest;
import com.jjxmas.ainovelstudio.module.chapter.dto.ChapterGenerateRequest;
import com.jjxmas.ainovelstudio.module.chapter.dto.ChapterResponse;
import com.jjxmas.ainovelstudio.module.chapter.dto.ChapterRewriteRequest;
import java.util.List;

public interface ChapterService {

    List<ChapterResponse> listChapters(Long projectId);

    ChapterResponse confirmChapterOutline(Long chapterId);

    ChapterResponse generateChapter(ChapterGenerateRequest request);

    ChapterResponse updateChapterContent(Long chapterId, ChapterContentUpdateRequest request);

    ChapterResponse rewriteChapter(Long chapterId, ChapterRewriteRequest request);
}

