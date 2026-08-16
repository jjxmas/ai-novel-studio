package com.jjxmas.ainovelstudio.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jjxmas.ainovelstudio.common.util.JsonUtils;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterQualityCheckResult;
import com.jjxmas.ainovelstudio.pojo.entity.GenerationJob;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GenerationJobWorkerTests {

    private GenerationJobService generationJobService;
    private ChapterPostProcessService chapterPostProcessService;
    private ChapterGenerationBatchService chapterGenerationBatchService;
    private StoryRebuildService storyRebuildService;
    private GenerationJobWorker worker;

    @BeforeEach
    void setUp() {
        generationJobService = mock(GenerationJobService.class);
        chapterPostProcessService = mock(ChapterPostProcessService.class);
        chapterGenerationBatchService = mock(ChapterGenerationBatchService.class);
        storyRebuildService = mock(StoryRebuildService.class);
        worker = new GenerationJobWorker(
                generationJobService,
                chapterPostProcessService,
                chapterGenerationBatchService,
                storyRebuildService,
                "worker-test",
                3,
                1000,
                60000);
    }

    @Test
    void processesChapterPostProcessJobAndCompletesClaim() {
        GenerationJob job = job("chapter_post_process", 1);
        when(generationJobService.claimNext(eq("worker-test"), any(LocalDateTime.class))).thenReturn(job);
        when(chapterPostProcessService.refreshQueuedChapter(20L, 4, 7L, "manual_edit", "调整正文", 10L))
                .thenReturn(ChapterQualityCheckResult.builder()
                        .status("completed")
                        .issueCount(2)
                        .build());

        worker.processNext();

        verify(generationJobService).completeJob(
                eq(10L), eq("worker-test"), any(Map.class), any(LocalDateTime.class));
        verify(generationJobService, never()).failJob(any(), any(), any(), any(Boolean.class), any(), any());
    }

    @Test
    void unsupportedJobIsReturnedForRetry() {
        GenerationJob job = job("unknown", 1);
        when(generationJobService.claimNext(eq("worker-test"), any(LocalDateTime.class))).thenReturn(job);

        worker.processNext();

        verify(generationJobService).failJob(
                eq(10L),
                eq("worker-test"),
                eq("UNSUPPORTED_GENERATION_JOB_TYPE: unknown"),
                eq(true),
                any(LocalDateTime.class),
                any(LocalDateTime.class));
        verify(generationJobService, never()).completeJob(any(), any(), any(), any());
    }

    @Test
    void processesChapterGenerationBatchAndHeartbeats() {
        GenerationJob job = job("chapter_generation_batch", 1)
                .setRelatedEntityType("generation_batch")
                .setRelatedEntityId(100L);
        when(generationJobService.claimNext(eq("worker-test"), any(LocalDateTime.class))).thenReturn(job);
        when(chapterGenerationBatchService.processQueuedBatch(eq(100L), any(Runnable.class)))
                .thenAnswer(invocation -> {
                    invocation.<Runnable>getArgument(1).run();
                    return "completed";
                });

        worker.processNext();

        verify(generationJobService).heartbeat(eq(10L), eq("worker-test"), any(LocalDateTime.class));
        verify(generationJobService).completeJob(
                eq(10L), eq("worker-test"), any(Map.class), any(LocalDateTime.class));
    }

    @Test
    void processesQualityCheckBatchWithSameDurableWorker() {
        GenerationJob job = job("quality_check_batch", 1)
                .setRelatedEntityType("generation_batch")
                .setRelatedEntityId(101L);
        when(generationJobService.claimNext(eq("worker-test"), any(LocalDateTime.class))).thenReturn(job);
        when(chapterGenerationBatchService.processQueuedBatch(eq(101L), any(Runnable.class)))
                .thenReturn("completed");

        worker.processNext();

        verify(chapterGenerationBatchService).processQueuedBatch(eq(101L), any(Runnable.class));
        verify(generationJobService).completeJob(
                eq(10L), eq("worker-test"), any(Map.class), any(LocalDateTime.class));
    }

    @Test
    void terminalBatchQueueFailureMarksBatchFailed() {
        GenerationJob job = job("chapter_generation_batch", 3)
                .setRelatedEntityType("generation_batch")
                .setRelatedEntityId(100L);
        when(generationJobService.claimNext(eq("worker-test"), any(LocalDateTime.class))).thenReturn(job);
        when(chapterGenerationBatchService.processQueuedBatch(eq(100L), any(Runnable.class)))
                .thenThrow(new IllegalStateException("database unavailable"));

        worker.processNext();

        verify(chapterGenerationBatchService).markQueueFailure(100L, "database unavailable");
        verify(generationJobService).failJob(
                eq(10L),
                eq("worker-test"),
                eq("database unavailable"),
                eq(false),
                eq(null),
                any(LocalDateTime.class));
    }

    @Test
    void processesStoryRebuildWithHeartbeat() {
        GenerationJob job = job("story_rebuild", 1)
                .setRelatedEntityType("story_rebuild_run")
                .setRelatedEntityId(50L);
        when(generationJobService.claimNext(eq("worker-test"), any(LocalDateTime.class))).thenReturn(job);
        when(storyRebuildService.processQueuedRebuild(eq(50L), any(Runnable.class)))
                .thenAnswer(invocation -> {
                    invocation.<Runnable>getArgument(1).run();
                    return com.jjxmas.ainovelstudio.pojo.dto.StoryRebuildResult.builder()
                            .status("completed")
                            .processedChapterCount(8)
                            .skippedChapterCount(1)
                            .build();
                });

        worker.processNext();

        verify(generationJobService).heartbeat(eq(10L), eq("worker-test"), any(LocalDateTime.class));
        verify(generationJobService).completeJob(
                eq(10L), eq("worker-test"), any(Map.class), any(LocalDateTime.class));
    }

    private GenerationJob job(String jobType, int attempts) {
        GenerationJob job = new GenerationJob()
                .setProjectId(1L)
                .setJobType(jobType)
                .setRelatedEntityType("chapter")
                .setRelatedEntityId(20L)
                .setModelConfigId(7L)
                .setAttemptCount(attempts)
                .setInputSnapshot(JsonUtils.toJson(Map.of(
                        "contentVersion", 4,
                        "dirtyReason", "manual_edit",
                        "dirtyNote", "调整正文")));
        job.setId(10L);
        return job;
    }
}
