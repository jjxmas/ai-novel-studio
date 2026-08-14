package com.jjxmas.ainovelstudio.service;

public interface ChapterPostProcessService {

    void refreshChapter(Long chapterId, Long modelConfigId);

    void refreshChapterAndMarkDirty(Long chapterId, Long modelConfigId, String dirtyReason, String dirtyNote);

    void refreshChapterAsync(Long chapterId, Long modelConfigId);

    void refreshChapterAndMarkDirtyAsync(Long chapterId, Long modelConfigId, String dirtyReason, String dirtyNote);
}
