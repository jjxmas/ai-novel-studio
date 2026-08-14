package com.jjxmas.ainovelstudio.service;

import com.jjxmas.ainovelstudio.pojo.dto.ChapterContentUpdateRequest;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterGenerateRequest;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterGenerationResult;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterResponse;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterRewriteRequest;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterStreamEvent;
import java.util.List;
import reactor.core.publisher.Flux;

public interface ChapterService {

    List<ChapterResponse> listChapters(Long projectId);

    void confirmChapterOutline(Long chapterId);

    ChapterResponse generateChapter(ChapterGenerateRequest request);

    ChapterGenerationResult generateChapterForBatch(ChapterGenerateRequest request);

    Flux<ChapterStreamEvent> streamGenerateChapter(ChapterGenerateRequest request);

    ChapterResponse updateChapterContent(Long chapterId, ChapterContentUpdateRequest request);

    ChapterResponse rewriteChapter(Long chapterId, ChapterRewriteRequest request);

    Flux<ChapterStreamEvent> streamRewriteChapter(Long chapterId, ChapterRewriteRequest request);
}
