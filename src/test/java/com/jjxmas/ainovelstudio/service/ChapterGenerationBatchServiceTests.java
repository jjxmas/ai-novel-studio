package com.jjxmas.ainovelstudio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
import com.jjxmas.ainovelstudio.pojo.dto.CheckRequest;
import com.jjxmas.ainovelstudio.pojo.entity.Chapter;
import com.jjxmas.ainovelstudio.pojo.entity.GenerationBatch;
import com.jjxmas.ainovelstudio.pojo.entity.GenerationBatchItem;
import com.jjxmas.ainovelstudio.pojo.entity.Project;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class ChapterGenerationBatchServiceTests {

    @Test
    void createsQualityCheckBatchOnExistingDatabaseQueue() {
        GenerationBatchMapper batchMapper = mock(GenerationBatchMapper.class);
        GenerationBatchItemMapper itemMapper = mock(GenerationBatchItemMapper.class);
        ProjectMapper projectMapper = mock(ProjectMapper.class);
        ChapterMapper chapterMapper = mock(ChapterMapper.class);
        ChapterService chapterService = mock(ChapterService.class);
        CheckService checkService = mock(CheckService.class);
        GenerationJobService generationJobService = mock(GenerationJobService.class);
        when(projectMapper.selectById(1L)).thenReturn(new Project());
        when(batchMapper.selectOne(any())).thenReturn(null);
        when(chapterMapper.selectList(any())).thenReturn(List.of(
                chapter(10L, 1, "第一章正文"),
                chapter(20L, 2, "第二章正文")));
        when(batchMapper.insert(any(GenerationBatch.class))).thenAnswer(invocation -> {
            invocation.<GenerationBatch>getArgument(0).setId(100L);
            return 1;
        });
        when(itemMapper.insert(any(GenerationBatchItem.class))).thenReturn(1);

        ChapterGenerationBatchService service = new ChapterGenerationBatchService(
                batchMapper,
                itemMapper,
                projectMapper,
                chapterMapper,
                chapterService,
                checkService,
                generationJobService);
        CheckRequest request = new CheckRequest();
        request.setProjectId(1L);
        request.setModelConfigId(7L);
        request.setCheckType("all");

        var response = service.createQualityCheckBatch(request);

        assertThat(response.getBatchType()).isEqualTo("quality_check");
        assertThat(response.getTotalCount()).isEqualTo(2);
        assertThat(response.getItems()).hasSize(2);
        verify(generationJobService).enqueueJob(
                eq(1L),
                eq("quality_check_batch"),
                eq("generation_batch"),
                eq(100L),
                eq(7L),
                any(Map.class),
                eq("100:1"),
                eq(0),
                any(LocalDateTime.class));
    }

    @Test
    void qualityCheckBatchChecksChaptersWithoutRegeneratingContent() {
        GenerationBatchMapper batchMapper = mock(GenerationBatchMapper.class);
        GenerationBatchItemMapper itemMapper = mock(GenerationBatchItemMapper.class);
        ProjectMapper projectMapper = mock(ProjectMapper.class);
        ChapterMapper chapterMapper = mock(ChapterMapper.class);
        ChapterService chapterService = mock(ChapterService.class);
        CheckService checkService = mock(CheckService.class);
        GenerationJobService generationJobService = mock(GenerationJobService.class);
        GenerationBatch batch = new GenerationBatch()
                .setProjectId(1L)
                .setBatchType("quality_check")
                .setModelConfigId(7L)
                .setStatus("queued")
                .setRunNo(1)
                .setTotalCount(2)
                .setPendingCount(2)
                .setRunningCount(0)
                .setSucceededCount(0)
                .setFailedCount(0)
                .setSkippedCount(0)
                .setQualityCheckedCount(0)
                .setQualityFailedCount(0)
                .setQualityIssueCount(0)
                .setRequestSnapshot(JsonUtils.toJson(Map.of("checkType", "all")));
        batch.setId(100L);
        List<GenerationBatchItem> items = new ArrayList<>();
        for (int chapterNo = 1; chapterNo <= 2; chapterNo++) {
            GenerationBatchItem item = new GenerationBatchItem()
                    .setBatchId(100L)
                    .setProjectId(1L)
                    .setChapterId((long) chapterNo * 10)
                    .setChapterNo(chapterNo)
                    .setItemType("quality_check")
                    .setStatus("pending")
                    .setAttemptCount(0)
                    .setQualityStatus("pending")
                    .setQualityIssueCount(0);
            item.setId((long) chapterNo);
            items.add(item);
        }
        when(batchMapper.selectById(100L)).thenReturn(batch);
        when(batchMapper.updateById(any(GenerationBatch.class))).thenReturn(1);
        when(itemMapper.selectList(any())).thenReturn(items);
        when(itemMapper.selectById(anyLong())).thenAnswer(invocation -> items.stream()
                .filter(item -> item.getId().equals(invocation.getArgument(0)))
                .findFirst()
                .orElse(null));
        when(itemMapper.updateById(any(GenerationBatchItem.class))).thenReturn(1);
        when(checkService.runCheck(any())).thenReturn(CheckResponse.builder()
                .issueCount(1)
                .issues(List.of())
                .summary("检查完成")
                .build());

        ChapterGenerationBatchService service = new ChapterGenerationBatchService(
                batchMapper,
                itemMapper,
                projectMapper,
                chapterMapper,
                chapterService,
                checkService,
                generationJobService);

        service.processBatch(100L);

        verify(checkService, times(2)).runCheck(any(CheckRequest.class));
        verify(chapterService, never()).generateChapterForBatch(any());
        assertThat(batch.getStatus()).isEqualTo("completed");
        assertThat(batch.getQualityCheckedCount()).isEqualTo(2);
        assertThat(batch.getQualityIssueCount()).isEqualTo(2);
        assertThat(items).extracting(GenerationBatchItem::getStatus)
                .containsExactly("succeeded", "succeeded");
    }

    @Test
    void listsRecentBatchSummariesWithFailureDetails() {
        GenerationBatchMapper batchMapper = mock(GenerationBatchMapper.class);
        GenerationBatchItemMapper itemMapper = mock(GenerationBatchItemMapper.class);
        ProjectMapper projectMapper = mock(ProjectMapper.class);
        ChapterMapper chapterMapper = mock(ChapterMapper.class);
        ChapterService chapterService = mock(ChapterService.class);
        CheckService checkService = mock(CheckService.class);
        GenerationJobService generationJobService = mock(GenerationJobService.class);

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
                checkService,
                generationJobService);
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
    }

    @Test
    void continuesAfterItemFailureAndRetriesOnlyFailedItem() throws InterruptedException {
        GenerationBatchMapper batchMapper = mock(GenerationBatchMapper.class);
        GenerationBatchItemMapper itemMapper = mock(GenerationBatchItemMapper.class);
        ProjectMapper projectMapper = mock(ProjectMapper.class);
        ChapterMapper chapterMapper = mock(ChapterMapper.class);
        ChapterService chapterService = mock(ChapterService.class);
        CheckService checkService = mock(CheckService.class);
        GenerationJobService generationJobService = mock(GenerationJobService.class);

        GenerationBatch batch = new GenerationBatch()
                .setProjectId(1L)
                .setBatchType("chapter_content")
                .setModelConfigId(7L)
                .setStatus("queued")
                .setRunNo(1)
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

        when(batchMapper.selectById(100L)).thenReturn(batch);
        when(batchMapper.updateById(any(GenerationBatch.class))).thenReturn(1);
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
        ChapterGenerationBatchService service = new ChapterGenerationBatchService(
                batchMapper,
                itemMapper,
                projectMapper,
                chapterMapper,
                chapterService,
                checkService,
                generationJobService);
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
        verify(generationJobService).enqueueJob(
                eq(1L),
                eq("chapter_generation_batch"),
                eq("generation_batch"),
                eq(100L),
                eq(7L),
                any(Map.class),
                eq("100:2"),
                eq(0),
                any(LocalDateTime.class));
        service.processBatch(100L);

        assertThat(executionOrder).containsExactly(1, 2, 3, 2);
        assertThat(batch.getStatus()).isEqualTo("completed");
        assertThat(items.get(1).getStatus()).isEqualTo("succeeded");
        assertThat(items.get(1).getAttemptCount()).isEqualTo(2);
        assertThat(items.get(1).getQualityStatus()).isEqualTo("completed");
        assertThat(batch.getQualityCheckedCount()).isEqualTo(3);
        assertThat(batch.getQualityIssueCount()).isEqualTo(3);
    }

    private Chapter chapter(Long id, int chapterNo, String content) {
        Chapter chapter = new Chapter()
                .setProjectId(1L)
                .setChapterNo(chapterNo)
                .setContent(content);
        chapter.setId(id);
        return chapter;
    }
}
