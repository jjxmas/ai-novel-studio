package com.jjxmas.ainovelstudio.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jjxmas.ainovelstudio.mapper.ChapterMapper;
import com.jjxmas.ainovelstudio.mapper.ChapterPostProcessRunMapper;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterQualityCheckResult;
import com.jjxmas.ainovelstudio.pojo.dto.CheckRequest;
import com.jjxmas.ainovelstudio.pojo.dto.CheckResponse;
import com.jjxmas.ainovelstudio.pojo.entity.Chapter;
import com.jjxmas.ainovelstudio.pojo.entity.ChapterPostProcessRun;
import com.jjxmas.ainovelstudio.service.ChapterMemoryService;
import com.jjxmas.ainovelstudio.service.ChapterPostProcessService;
import com.jjxmas.ainovelstudio.service.CheckService;
import com.jjxmas.ainovelstudio.service.GenerationJobService;
import com.jjxmas.ainovelstudio.service.StoryDirtyMarkService;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ChapterPostProcessServiceImpl implements ChapterPostProcessService {

    private static final Logger log = LoggerFactory.getLogger(ChapterPostProcessServiceImpl.class);
    private static final int STEP_FACT_PROJECTION = 1;
    private static final int STEP_NARRATIVE_MEMORY = 2;
    private static final int STEP_DIRTY_MARK = 3;
    private static final int STEP_QUALITY_CHECK = 4;

    private final ChapterMapper chapterMapper;
    private final ChapterPostProcessRunMapper postProcessRunMapper;
    private final ChapterMemoryService chapterMemoryService;
    private final StoryDirtyMarkService storyDirtyMarkService;
    private final GenerationJobService generationJobService;
    private final CheckService checkService;

    public ChapterPostProcessServiceImpl(
            ChapterMapper chapterMapper,
            ChapterPostProcessRunMapper postProcessRunMapper,
            ChapterMemoryService chapterMemoryService,
            StoryDirtyMarkService storyDirtyMarkService,
            GenerationJobService generationJobService,
            CheckService checkService) {
        this.chapterMapper = chapterMapper;
        this.postProcessRunMapper = postProcessRunMapper;
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
    public void enqueueChapter(Long chapterId, Long modelConfigId) {
        enqueue(chapterId, modelConfigId, null, null);
    }

    @Override
    public void enqueueChapterAndMarkDirty(
            Long chapterId,
            Long modelConfigId,
            String dirtyReason,
            String dirtyNote) {
        enqueue(chapterId, modelConfigId, dirtyReason, dirtyNote);
    }

    @Override
    public ChapterQualityCheckResult refreshQueuedChapter(
            Long chapterId,
            int expectedContentVersion,
            Long modelConfigId,
            String dirtyReason,
            String dirtyNote,
            Long generationJobId) {
        Chapter chapter = requireChapter(chapterId);
        if (contentVersion(chapter) != expectedContentVersion) {
            return ChapterQualityCheckResult.builder()
                    .status("skipped")
                    .issueCount(0)
                    .build();
        }
        ChapterPostProcessRun run = requireRun(chapter, expectedContentVersion, generationJobId);
        try {
            run.setStatus("running").setErrorMessage(null).setGenerationJobId(generationJobId);
            postProcessRunMapper.updateById(run);
            if (step(run) < STEP_FACT_PROJECTION) {
                chapterMemoryService.refreshFactProjection(chapter, modelConfigId);
                advance(run, STEP_FACT_PROJECTION);
            }
            if (step(run) < STEP_NARRATIVE_MEMORY) {
                chapterMemoryService.refreshNarrativeMemory(chapter, modelConfigId);
                advance(run, STEP_NARRATIVE_MEMORY);
            }
            if (step(run) < STEP_DIRTY_MARK) {
                if (dirtyReason != null && !dirtyReason.isBlank()) {
                    storyDirtyMarkService.markDownstreamDirty(chapter, dirtyReason, dirtyNote);
                }
                advance(run, STEP_DIRTY_MARK);
            }
            if (step(run) < STEP_QUALITY_CHECK) {
                ChapterQualityCheckResult result = runContinuityCheck(chapter, modelConfigId);
                run.setQualityStatus(result.getStatus())
                        .setQualityIssueCount(result.getIssueCount() == null ? 0 : result.getIssueCount())
                        .setQualityErrorMessage(result.getErrorMessage());
                advance(run, STEP_QUALITY_CHECK);
            }
            run.setStatus("completed");
            postProcessRunMapper.updateById(run);
            return savedQualityResult(run);
        } catch (RuntimeException ex) {
            run.setStatus("failed").setErrorMessage(errorMessage(ex));
            postProcessRunMapper.updateById(run);
            throw ex;
        }
    }

    private ChapterPostProcessRun requireRun(
            Chapter chapter,
            int contentVersion,
            Long generationJobId) {
        ChapterPostProcessRun run = postProcessRunMapper.selectOne(new LambdaQueryWrapper<ChapterPostProcessRun>()
                .eq(ChapterPostProcessRun::getChapterId, chapter.getId())
                .eq(ChapterPostProcessRun::getContentVersionNo, contentVersion)
                .last("LIMIT 1"));
        if (run != null) {
            return run;
        }
        run = new ChapterPostProcessRun()
                .setProjectId(chapter.getProjectId())
                .setChapterId(chapter.getId())
                .setContentVersionNo(contentVersion)
                .setGenerationJobId(generationJobId)
                .setStatus("running")
                .setCompletedStep(0)
                .setQualityIssueCount(0);
        postProcessRunMapper.insert(run);
        return run;
    }

    private void advance(ChapterPostProcessRun run, int completedStep) {
        run.setCompletedStep(completedStep);
        postProcessRunMapper.updateById(run);
    }

    private ChapterQualityCheckResult savedQualityResult(ChapterPostProcessRun run) {
        return ChapterQualityCheckResult.builder()
                .status(run.getQualityStatus() == null ? "completed" : run.getQualityStatus())
                .issueCount(run.getQualityIssueCount() == null ? 0 : run.getQualityIssueCount())
                .errorMessage(run.getQualityErrorMessage())
                .build();
    }

    private int step(ChapterPostProcessRun run) {
        return run.getCompletedStep() == null ? 0 : run.getCompletedStep();
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
        return doRefreshChapter(requireChapter(chapterId), modelConfigId, dirtyReason, dirtyNote);
    }

    private ChapterQualityCheckResult doRefreshChapter(
            Chapter chapter,
            Long modelConfigId,
            String dirtyReason,
            String dirtyNote) {
        chapterMemoryService.refreshAfterChapterContent(chapter, modelConfigId);
        if (dirtyReason != null && !dirtyReason.isBlank()) {
            storyDirtyMarkService.markDownstreamDirty(chapter, dirtyReason, dirtyNote);
        }
        return runContinuityCheck(chapter, modelConfigId);
    }

    private ChapterQualityCheckResult runContinuityCheck(Chapter chapter, Long modelConfigId) {
        CheckRequest request = new CheckRequest();
        request.setProjectId(chapter.getProjectId());
        request.setChapterId(chapter.getId());
        request.setModelConfigId(modelConfigId);
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

    private void enqueue(Long chapterId, Long modelConfigId, String dirtyReason, String dirtyNote) {
        Chapter chapter = requireChapter(chapterId);
        generationJobService.enqueueJob(
                chapter.getProjectId(),
                "chapter_post_process",
                "chapter",
                chapterId,
                modelConfigId,
                Map.of(
                        "contentVersion", contentVersion(chapter),
                        "dirtyReason", dirtyReason == null ? "" : dirtyReason,
                        "dirtyNote", dirtyNote == null ? "" : dirtyNote),
                chapterId + ":" + contentVersion(chapter),
                0,
                java.time.LocalDateTime.now());
    }

    private Chapter requireChapter(Long chapterId) {
        Chapter chapter = chapterMapper.selectById(chapterId);
        if (chapter == null) {
            throw new IllegalArgumentException("Chapter does not exist: " + chapterId);
        }
        return chapter;
    }

    private int contentVersion(Chapter chapter) {
        return chapter.getLastContentVersionNo() == null ? 0 : chapter.getLastContentVersionNo();
    }

    private String errorMessage(RuntimeException exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
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
