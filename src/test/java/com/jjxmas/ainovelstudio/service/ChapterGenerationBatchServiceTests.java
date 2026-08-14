package com.jjxmas.ainovelstudio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jjxmas.ainovelstudio.common.util.JsonUtils;
import com.jjxmas.ainovelstudio.mapper.ChapterMapper;
import com.jjxmas.ainovelstudio.mapper.GenerationBatchItemMapper;
import com.jjxmas.ainovelstudio.mapper.GenerationBatchMapper;
import com.jjxmas.ainovelstudio.mapper.ProjectMapper;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterGenerationResult;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterQualityCheckResult;
import com.jjxmas.ainovelstudio.pojo.dto.CheckIssueResponse;
import com.jjxmas.ainovelstudio.pojo.dto.CheckResponse;
import com.jjxmas.ainovelstudio.pojo.entity.GenerationBatch;
import com.jjxmas.ainovelstudio.pojo.entity.GenerationBatchItem;
import com.jjxmas.ainovelstudio.pojo.entity.Project;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class ChapterGenerationBatchServiceTests {

    @Test
    void listsRecentBatchSummariesWithFailureDetails() {
        GenerationBatchMapper batchMapper = mock(GenerationBatchMapper.class);
        GenerationBatchItemMapper itemMapper = mock(GenerationBatchItemMapper.class);
        ProjectMapper projectMapper = mock(ProjectMapper.class);
        ChapterMapper chapterMapper = mock(ChapterMapper.class);
        ChapterService chapterService = mock(ChapterService.class);
        ProjectChapterGenerationQueue projectQueue = mock(ProjectChapterGenerationQueue.class);

        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 14, 12, 0);
        GenerationBatch failedBatch = new GenerationBatch()
                .setProjectId(1L)
                .setBatchType("chapter_content")
                .setModelConfigId(7L)
                .setStatus("partial_failed")
                .setTotalCount(3)
                .setPendingCount(0)
                .setRunningCount(0)
                .setSucceededCount(2)
                .setFailedCount(1)
                .setSkippedCount(0)
                .setQualityCheckedCount(2)
                .setQualityFailedCount(0)
                .setQualityIssueCount(2)
                .setErrorMessage("一章生成失败");
        failedBatch.setId(101L);
        failedBatch.setCreatedAt(createdAt);
        when(projectMapper.selectById(1L)).thenReturn(new Project());
        when(batchMapper.selectList(any())).thenReturn(List.of(failedBatch));

        ChapterGenerationBatchService service = new ChapterGenerationBatchService(
                batchMapper,
                itemMapper,
                projectMapper,
                chapterMapper,
                chapterService,
                projectQueue,
                false);
        try {
            assertThat(service.listBatches(1L)).singleElement().satisfies(summary -> {
                assertThat(summary.getBatchId()).isEqualTo(101L);
                assertThat(summary.getBatchType()).isEqualTo("chapter_content");
                assertThat(summary.getStatus()).isEqualTo("partial_failed");
                assertThat(summary.getFailedCount()).isEqualTo(1);
                assertThat(summary.getQualityCheckedCount()).isEqualTo(2);
                assertThat(summary.getQualityIssueCount()).isEqualTo(2);
                assertThat(summary.getErrorMessage()).isEqualTo("一章生成失败");
                assertThat(summary.getCreatedAt()).isEqualTo(createdAt);
            });
        } finally {
            service.shutdown();
        }
    }

    @Test
    void continuesAfterItemFailureAndRetriesOnlyFailedItem() throws InterruptedException {
        GenerationBatchMapper batchMapper = mock(GenerationBatchMapper.class);
        GenerationBatchItemMapper itemMapper = mock(GenerationBatchItemMapper.class);
        ProjectMapper projectMapper = mock(ProjectMapper.class);
        ChapterMapper chapterMapper = mock(ChapterMapper.class);
        ChapterService chapterService = mock(ChapterService.class);
        ProjectChapterGenerationQueue projectQueue = mock(ProjectChapterGenerationQueue.class);

        GenerationBatch batch = new GenerationBatch()
                .setProjectId(1L)
                .setBatchType("chapter_content")
                .setModelConfigId(7L)
                .setStatus("queued")
                .setTotalCount(3)
                .setPendingCount(3)
                .setRunningCount(0)
                .setSucceededCount(0)
                .setFailedCount(0)
                .setSkippedCount(0)
                .setQualityCheckedCount(0)
                .setQualityFailedCount(0)
                .setQualityIssueCount(0)
                .setRequestSnapshot(JsonUtils.toJson(Map.of("instruction", "保持节奏")));
        batch.setId(100L);
        List<GenerationBatchItem> items = new ArrayList<>();
        for (int chapterNo = 1; chapterNo <= 3; chapterNo++) {
            GenerationBatchItem item = new GenerationBatchItem()
                    .setBatchId(100L)
                    .setProjectId(1L)
                    .setChapterId((long) chapterNo)
                    .setChapterNo(chapterNo)
                    .setItemType("chapter_content")
                    .setStatus("pending")
                    .setQualityStatus("pending")
                    .setQualityIssueCount(0)
                    .setAttemptCount(0);
            item.setId((long) chapterNo);
            items.add(item);
        }

        CountDownLatch retryCompleted = new CountDownLatch(1);
        when(batchMapper.selectById(100L)).thenReturn(batch);
        when(batchMapper.updateById(any(GenerationBatch.class))).thenAnswer(invocation -> {
            GenerationBatch updated = invocation.getArgument(0);
            if ("completed".equals(updated.getStatus())) {
                retryCompleted.countDown();
            }
            return 1;
        });
        when(itemMapper.selectList(any())).thenAnswer(invocation -> items);
        when(itemMapper.selectById(anyLong())).thenAnswer(invocation -> items.stream()
                .filter(item -> item.getId().equals(invocation.getArgument(0)))
                .findFirst()
                .orElse(null));
        when(itemMapper.updateById(any(GenerationBatchItem.class))).thenReturn(1);

        AtomicBoolean failSecondChapter = new AtomicBoolean(true);
        List<Integer> executionOrder = new ArrayList<>();
        CheckResponse qualityReport = CheckResponse.builder()
                .issueCount(1)
                .issues(List.of(CheckIssueResponse.builder()
                        .type("continuity")
                        .severity("medium")
                        .description("承接关系需要确认")
                        .suggestion("补充上一章行动结果")
                        .build()))
                .summary("连续性检查完成")
                .build();
        ChapterQualityCheckResult qualityCheck = ChapterQualityCheckResult.builder()
                .status("completed")
                .issueCount(1)
                .report(qualityReport)
                .build();
        when(chapterService.generateChapterForBatch(any())).thenAnswer(invocation -> {
            int chapterNo = invocation.<com.jjxmas.ainovelstudio.pojo.dto.ChapterGenerateRequest>getArgument(0)
                    .getChapterNo();
            executionOrder.add(chapterNo);
            if (chapterNo == 2 && failSecondChapter.get()) {
                throw new IllegalStateException("模拟生成失败");
            }
            return new ChapterGenerationResult(null, 1000L + chapterNo, qualityCheck);
        });
        when(projectQueue.enqueueTask(anyLong(), any(Runnable.class))).thenAnswer(invocation -> {
            invocation.<Runnable>getArgument(1).run();
            return Mono.empty();
        });

        ChapterGenerationBatchService service = new ChapterGenerationBatchService(
                batchMapper,
                itemMapper,
                projectMapper,
                chapterMapper,
                chapterService,
                projectQueue,
                false);
        try {
            service.processBatch(100L);

            assertThat(executionOrder).containsExactly(1, 2, 3);
            assertThat(batch.getStatus()).isEqualTo("partial_failed");
            assertThat(items).extracting(GenerationBatchItem::getStatus)
                    .containsExactly("succeeded", "failed", "succeeded");
            assertThat(batch.getSucceededCount()).isEqualTo(2);
            assertThat(batch.getFailedCount()).isEqualTo(1);
            assertThat(batch.getQualityCheckedCount()).isEqualTo(2);
            assertThat(batch.getQualityIssueCount()).isEqualTo(2);

            failSecondChapter.set(false);
            service.retryFailed(100L);

            assertThat(retryCompleted.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(executionOrder).containsExactly(1, 2, 3, 2);
            assertThat(batch.getStatus()).isEqualTo("completed");
            assertThat(items.get(1).getStatus()).isEqualTo("succeeded");
            assertThat(items.get(1).getAttemptCount()).isEqualTo(2);
            assertThat(items.get(1).getQualityStatus()).isEqualTo("completed");
            assertThat(batch.getQualityCheckedCount()).isEqualTo(3);
            assertThat(batch.getQualityIssueCount()).isEqualTo(3);
        } finally {
            service.shutdown();
        }
    }
}
