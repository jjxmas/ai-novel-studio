package com.jjxmas.ainovelstudio.service;

import com.jjxmas.ainovelstudio.common.util.JsonUtils;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterQualityCheckResult;
import com.jjxmas.ainovelstudio.pojo.entity.GenerationJob;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.jobs.enabled", havingValue = "true", matchIfMissing = true)
public class GenerationJobWorker {

    private static final Logger log = LoggerFactory.getLogger(GenerationJobWorker.class);

    private final GenerationJobService generationJobService;
    private final ChapterPostProcessService chapterPostProcessService;
    private final ChapterGenerationBatchService chapterGenerationBatchService;
    private final StoryRebuildService storyRebuildService;
    private final String workerId;
    private final int maxAttempts;
    private final long retryDelayMs;
    private final long leaseTimeoutMs;

    public GenerationJobWorker(
            GenerationJobService generationJobService,
            ChapterPostProcessService chapterPostProcessService,
            ChapterGenerationBatchService chapterGenerationBatchService,
            StoryRebuildService storyRebuildService,
            @Value("${app.jobs.worker-id:}") String workerId,
            @Value("${app.jobs.max-attempts:3}") int maxAttempts,
            @Value("${app.jobs.retry-delay-ms:30000}") long retryDelayMs,
            @Value("${app.jobs.lease-timeout-ms:7200000}") long leaseTimeoutMs) {
        this.generationJobService = generationJobService;
        this.chapterPostProcessService = chapterPostProcessService;
        this.chapterGenerationBatchService = chapterGenerationBatchService;
        this.storyRebuildService = storyRebuildService;
        this.workerId = workerId == null || workerId.isBlank()
                ? "worker-" + UUID.randomUUID()
                : workerId;
        this.maxAttempts = maxAttempts;
        this.retryDelayMs = retryDelayMs;
        this.leaseTimeoutMs = leaseTimeoutMs;
    }

    @Scheduled(
            initialDelayString = "${app.jobs.initial-delay-ms:30000}",
            fixedDelayString = "${app.jobs.poll-interval-ms:1000}")
    public void processNext() {
        LocalDateTime now = LocalDateTime.now();
        GenerationJob job = generationJobService.claimNext(workerId, now);
        if (job == null) {
            return;
        }
        try {
            Map<String, Object> output = execute(job);
            generationJobService.completeJob(job.getId(), workerId, output, LocalDateTime.now());
        } catch (RuntimeException ex) {
            boolean retry = attempts(job) < maxAttempts;
            LocalDateTime failedAt = LocalDateTime.now();
            LocalDateTime retryAt = retry ? failedAt.plusNanos(retryDelayMs * 1_000_000) : null;
            if (!retry && isChapterBatch(job)) {
                chapterGenerationBatchService.markQueueFailure(job.getRelatedEntityId(), errorMessage(ex));
            }
            if ("story_rebuild".equals(job.getJobType())) {
                storyRebuildService.markQueueFailure(job.getRelatedEntityId(), errorMessage(ex), retry);
            }
            try {
                generationJobService.failJob(
                        job.getId(), workerId, errorMessage(ex), retry, retryAt, failedAt);
            } catch (RuntimeException transitionEx) {
                log.error("Failed to update generation job after execution error. jobId={}", job.getId(), transitionEx);
            }
            log.error("Generation job execution failed. jobId={}, type={}", job.getId(), job.getJobType(), ex);
        }
    }

    @Scheduled(
            initialDelayString = "${app.jobs.recovery-initial-delay-ms:60000}",
            fixedDelayString = "${app.jobs.recovery-interval-ms:60000}")
    public void recoverExpiredJobs() {
        LocalDateTime now = LocalDateTime.now();
        int recovered = generationJobService.recoverExpiredJobs(
                now.minusNanos(leaseTimeoutMs * 1_000_000), now);
        if (recovered > 0) {
            log.warn("Recovered {} expired generation jobs", recovered);
        }
    }

    private Map<String, Object> execute(GenerationJob job) {
        if (isChapterBatch(job)) {
            String status = chapterGenerationBatchService.processQueuedBatch(
                    job.getRelatedEntityId(),
                    () -> generationJobService.heartbeat(job.getId(), workerId, LocalDateTime.now()));
            return Map.of("batchId", job.getRelatedEntityId(), "status", status);
        }
        if ("chapter_post_process".equals(job.getJobType())) {
            return executeChapterPostProcess(job);
        }
        if ("story_rebuild".equals(job.getJobType())) {
            var result = storyRebuildService.processQueuedRebuild(
                    job.getRelatedEntityId(),
                    () -> generationJobService.heartbeat(job.getId(), workerId, LocalDateTime.now()));
            Map<String, Object> output = new LinkedHashMap<>();
            output.put("runId", job.getRelatedEntityId());
            output.put("status", result.getStatus());
            output.put("processedChapterCount", result.getProcessedChapterCount());
            output.put("skippedChapterCount", result.getSkippedChapterCount());
            return output;
        }
        throw new IllegalArgumentException("UNSUPPORTED_GENERATION_JOB_TYPE: " + job.getJobType());
    }

    private boolean isChapterBatch(GenerationJob job) {
        return "chapter_generation_batch".equals(job.getJobType())
                || "quality_check_batch".equals(job.getJobType());
    }

    private Map<String, Object> executeChapterPostProcess(GenerationJob job) {
        Map<String, Object> input = JsonUtils.toMap(job.getInputSnapshot());
        int expectedVersion = requiredInt(input, "contentVersion");
        ChapterQualityCheckResult result = chapterPostProcessService.refreshQueuedChapter(
                job.getRelatedEntityId(),
                expectedVersion,
                job.getModelConfigId(),
                text(input.get("dirtyReason")),
                text(input.get("dirtyNote")),
                job.getId());
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("status", result.getStatus());
        output.put("issueCount", result.getIssueCount());
        output.put("errorMessage", text(result.getErrorMessage()));
        return output;
    }

    private int requiredInt(Map<String, Object> input, String key) {
        Object value = input.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw new IllegalArgumentException("INVALID_GENERATION_JOB_INPUT: " + key);
    }

    private int attempts(GenerationJob job) {
        return job.getAttemptCount() == null ? 0 : job.getAttemptCount();
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String errorMessage(RuntimeException exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
    }
}
