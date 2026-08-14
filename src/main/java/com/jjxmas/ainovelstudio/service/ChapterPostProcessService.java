package com.jjxmas.ainovelstudio.service;

import com.jjxmas.ainovelstudio.pojo.dto.ChapterQualityCheckResult;

public interface ChapterPostProcessService {

    ChapterQualityCheckResult refreshChapter(Long chapterId, Long modelConfigId);

    ChapterQualityCheckResult refreshChapterAndMarkDirty(
            Long chapterId,
            Long modelConfigId,
            String dirtyReason,
            String dirtyNote);

    void refreshChapterAsync(Long chapterId, Long modelConfigId);

    void refreshChapterAndMarkDirtyAsync(Long chapterId, Long modelConfigId, String dirtyReason, String dirtyNote);
}
