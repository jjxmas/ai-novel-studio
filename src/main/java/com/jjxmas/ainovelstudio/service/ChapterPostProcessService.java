package com.jjxmas.ainovelstudio.service;

import com.jjxmas.ainovelstudio.pojo.dto.ChapterQualityCheckResult;

public interface ChapterPostProcessService {

    ChapterQualityCheckResult refreshChapter(Long chapterId, Long modelConfigId);

    ChapterQualityCheckResult refreshChapterAndMarkDirty(
            Long chapterId,
            Long modelConfigId,
            String dirtyReason,
            String dirtyNote);

    void enqueueChapter(Long chapterId, Long modelConfigId);

    void enqueueChapterAndMarkDirty(Long chapterId, Long modelConfigId, String dirtyReason, String dirtyNote);

    ChapterQualityCheckResult refreshQueuedChapter(
            Long chapterId,
            int expectedContentVersion,
            Long modelConfigId,
            String dirtyReason,
            String dirtyNote,
            Long generationJobId);
}
