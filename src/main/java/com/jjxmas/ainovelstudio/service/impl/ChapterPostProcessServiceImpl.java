package com.jjxmas.ainovelstudio.service.impl;

import com.jjxmas.ainovelstudio.mapper.ChapterMapper;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterQualityCheckResult;
import com.jjxmas.ainovelstudio.pojo.dto.CheckRequest;
import com.jjxmas.ainovelstudio.pojo.dto.CheckResponse;
import com.jjxmas.ainovelstudio.pojo.entity.Chapter;
import com.jjxmas.ainovelstudio.service.ChapterMemoryService;
import com.jjxmas.ainovelstudio.service.ChapterPostProcessService;
import com.jjxmas.ainovelstudio.service.CheckService;
import com.jjxmas.ainovelstudio.service.GenerationJobService;
import com.jjxmas.ainovelstudio.service.StoryDirtyMarkService;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ChapterPostProcessServiceImpl implements ChapterPostProcessService {

    private static final Logger log = LoggerFactory.getLogger(ChapterPostProcessServiceImpl.class);

    private final ChapterMapper chapterMapper;
    private final ChapterMemoryService chapterMemoryService;
    private final StoryDirtyMarkService storyDirtyMarkService;
    private final GenerationJobService generationJobService;
    private final CheckService checkService;

    public ChapterPostProcessServiceImpl(
            ChapterMapper chapterMapper,
            ChapterMemoryService chapterMemoryService,
            StoryDirtyMarkService storyDirtyMarkService,
            GenerationJobService generationJobService,
            CheckService checkService) {
        this.chapterMapper = chapterMapper;
        this.chapterMemoryService = chapterMemoryService;
        this.storyDirtyMarkService = storyDirtyMarkService;
        this.generationJobService = generationJobService;
        this.checkService = checkService;
    }

    @Override
    public ChapterQualityCheckResult refreshChapter(Long chapterId, Long modelConfigId) {
        return synchronouslyRefreshChapter(chapterId, modelConfigId, null, null);
    }

    @Override
    public ChapterQualityCheckResult refreshChapterAndMarkDirty(
            Long chapterId,
            Long modelConfigId,
            String dirtyReason,
            String dirtyNote) {
        return synchronouslyRefreshChapter(chapterId, modelConfigId, dirtyReason, dirtyNote);
    }

    @Override
    @Async
    public void refreshChapterAsync(Long chapterId, Long modelConfigId) {
        safelyRefreshChapter(chapterId, modelConfigId, null, null);
    }

    @Override
    @Async
    public void refreshChapterAndMarkDirtyAsync(Long chapterId, Long modelConfigId, String dirtyReason, String dirtyNote) {
        safelyRefreshChapter(chapterId, modelConfigId, dirtyReason, dirtyNote);
    }

    private void safelyRefreshChapter(Long chapterId, Long modelConfigId, String dirtyReason, String dirtyNote) {
        try {
            doRefreshChapter(chapterId, modelConfigId, dirtyReason, dirtyNote);
        } catch (RuntimeException ex) {
            log.error("Chapter post-process failed. chapterId={}, modelConfigId={}", chapterId, modelConfigId, ex);
            try {
                recordFailure(chapterId, modelConfigId, dirtyReason, dirtyNote, ex);
            } catch (RuntimeException recordEx) {
                log.error("Failed to record chapter post-process failure. chapterId={}", chapterId, recordEx);
            }
        }
    }

    private ChapterQualityCheckResult synchronouslyRefreshChapter(
            Long chapterId,
            Long modelConfigId,
            String dirtyReason,
            String dirtyNote) {
        try {
            return doRefreshChapter(chapterId, modelConfigId, dirtyReason, dirtyNote);
        } catch (RuntimeException ex) {
            try {
                recordFailure(chapterId, modelConfigId, dirtyReason, dirtyNote, ex);
            } catch (RuntimeException recordEx) {
                log.error("Failed to record chapter post-process failure. chapterId={}", chapterId, recordEx);
            }
            throw ex;
        }
    }

    private ChapterQualityCheckResult doRefreshChapter(
            Long chapterId,
            Long modelConfigId,
            String dirtyReason,
            String dirtyNote) {
        Chapter chapter = chapterMapper.selectById(chapterId);
        if (chapter == null) {
            throw new IllegalArgumentException("Chapter does not exist: " + chapterId);
        }
        chapterMemoryService.refreshAfterChapterContent(chapter, modelConfigId);
        if (dirtyReason != null && !dirtyReason.isBlank()) {
            storyDirtyMarkService.markDownstreamDirty(chapter, dirtyReason, dirtyNote);
        }
        return runContinuityCheck(chapter);
    }

    private ChapterQualityCheckResult runContinuityCheck(Chapter chapter) {
        CheckRequest request = new CheckRequest();
        request.setProjectId(chapter.getProjectId());
        request.setChapterId(chapter.getId());
        request.setCheckType("continuity");
        try {
            CheckResponse report = checkService.runCheck(request);
            return ChapterQualityCheckResult.builder()
                    .status("completed")
                    .issueCount(report.getIssueCount())
                    .report(report)
                    .build();
        } catch (RuntimeException ex) {
            log.error("Chapter continuity check failed. chapterId={}", chapter.getId(), ex);
            return ChapterQualityCheckResult.builder()
                    .status("failed")
                    .issueCount(0)
                    .errorMessage(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage())
                    .build();
        }
    }

    private void recordFailure(Long chapterId, Long modelConfigId, String dirtyReason, String dirtyNote, RuntimeException ex) {
        Chapter chapter = chapterMapper.selectById(chapterId);
        if (chapter == null) {
            return;
        }
        generationJobService.recordFailedJob(
                chapter.getProjectId(),
                "chapter_post_process",
                "chapter",
                chapterId,
                modelConfigId,
                Map.of(
                        "chapterId", chapterId,
                        "dirtyReason", dirtyReason == null ? "" : dirtyReason,
                        "dirtyNote", dirtyNote == null ? "" : dirtyNote),
                ex.getMessage());
    }
}
