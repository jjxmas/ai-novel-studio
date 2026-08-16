package com.jjxmas.ainovelstudio.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jjxmas.ainovelstudio.common.exception.BusinessException;
import com.jjxmas.ainovelstudio.common.exception.ErrorCode;
import com.jjxmas.ainovelstudio.common.util.JsonUtils;
import com.jjxmas.ainovelstudio.mapper.ChapterMapper;
import com.jjxmas.ainovelstudio.mapper.GenerationBatchItemMapper;
import com.jjxmas.ainovelstudio.mapper.GenerationBatchMapper;
import com.jjxmas.ainovelstudio.mapper.ProjectMapper;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterGenerateRequest;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterGenerationBatchCreateRequest;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterGenerationBatchItemResponse;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterGenerationBatchResponse;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterGenerationBatchSummaryResponse;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterGenerationResult;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterQualityCheckResult;
import com.jjxmas.ainovelstudio.pojo.dto.CheckResponse;
import com.jjxmas.ainovelstudio.pojo.dto.CheckRequest;
import com.jjxmas.ainovelstudio.pojo.entity.Chapter;
import com.jjxmas.ainovelstudio.pojo.entity.GenerationBatch;
import com.jjxmas.ainovelstudio.pojo.entity.GenerationBatchItem;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChapterGenerationBatchService {

    private static final Logger log = LoggerFactory.getLogger(ChapterGenerationBatchService.class);
    private static final Set<String> ACTIVE_STATUSES = Set.of("queued", "running", "paused", "cancel_requested");
    private static final Set<String> TERMINAL_STATUSES = Set.of("cancelled", "completed", "failed", "partial_failed");

    private final GenerationBatchMapper batchMapper;
    private final GenerationBatchItemMapper itemMapper;
    private final ProjectMapper projectMapper;
    private final ChapterMapper chapterMapper;
    private final ChapterService chapterService;
    private final CheckService checkService;
    private final GenerationJobService generationJobService;

    public ChapterGenerationBatchService(
            GenerationBatchMapper batchMapper,
            GenerationBatchItemMapper itemMapper,
            ProjectMapper projectMapper,
            ChapterMapper chapterMapper,
            ChapterService chapterService,
            CheckService checkService,
            GenerationJobService generationJobService) {
        this.batchMapper = batchMapper;
        this.itemMapper = itemMapper;
        this.projectMapper = projectMapper;
        this.chapterMapper = chapterMapper;
        this.chapterService = chapterService;
        this.checkService = checkService;
        this.generationJobService = generationJobService;
    }

    @Transactional
    public ChapterGenerationBatchResponse createBatch(
            Long projectId,
            ChapterGenerationBatchCreateRequest request) {
        if (projectMapper.selectById(projectId) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "作品不存在");
        }
        GenerationBatch activeBatch = batchMapper.selectOne(new LambdaQueryWrapper<GenerationBatch>()
                .eq(GenerationBatch::getProjectId, projectId)
                .in(GenerationBatch::getStatus, ACTIVE_STATUSES)
                .last("LIMIT 1"));
        if (activeBatch != null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前作品已有未完成的章节批次任务");
        }

        int endChapterNo = request.getStartChapterNo() + request.getCount() - 1;
        List<Chapter> chapters = chapterMapper.selectList(new LambdaQueryWrapper<Chapter>()
                .eq(Chapter::getProjectId, projectId)
                .between(Chapter::getChapterNo, request.getStartChapterNo(), endChapterNo)
                .orderByAsc(Chapter::getChapterNo));
        validateChapterRange(chapters, request.getStartChapterNo(), request.getCount());

        int skippedCount = (int) chapters.stream()
                .filter(chapter -> Boolean.TRUE.equals(request.getSkipExistingContent()) && hasContent(chapter))
                .count();
        GenerationBatch batch = new GenerationBatch()
                .setProjectId(projectId)
                .setBatchType("chapter_content")
                .setModelConfigId(request.getModelConfigId())
                .setStatus("queued")
                .setRunNo(1)
                .setTotalCount(request.getCount())
                .setPendingCount(request.getCount() - skippedCount)
                .setRunningCount(0)
                .setSucceededCount(0)
                .setFailedCount(0)
                .setSkippedCount(skippedCount)
                .setQualityCheckedCount(0)
                .setQualityFailedCount(0)
                .setQualityIssueCount(0)
                .setRequestSnapshot(JsonUtils.toJson(requestSnapshot(request)));
        batchMapper.insert(batch);

        List<GenerationBatchItem> items = new ArrayList<>(chapters.size());
        for (Chapter chapter : chapters) {
            boolean skipped = Boolean.TRUE.equals(request.getSkipExistingContent()) && hasContent(chapter);
            GenerationBatchItem item = new GenerationBatchItem()
                    .setBatchId(batch.getId())
                    .setProjectId(projectId)
                    .setChapterId(chapter.getId())
                    .setChapterNo(chapter.getChapterNo())
                    .setItemType("chapter_content")
                    .setStatus(skipped ? "skipped" : "pending")
                    .setAttemptCount(0)
                    .setQualityStatus(skipped ? "skipped" : "pending")
                    .setQualityIssueCount(0)
                    .setFinishedAt(skipped ? LocalDateTime.now() : null);
            itemMapper.insert(item);
            items.add(item);
        }
        enqueueBatch(batch);
        return toResponse(batch, items);
    }

    @Transactional
    public ChapterGenerationBatchResponse createQualityCheckBatch(CheckRequest request) {
        Long projectId = request.getProjectId();
        if (projectMapper.selectById(projectId) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "作品不存在");
        }
        GenerationBatch activeBatch = batchMapper.selectOne(new LambdaQueryWrapper<GenerationBatch>()
                .eq(GenerationBatch::getProjectId, projectId)
                .in(GenerationBatch::getStatus, ACTIVE_STATUSES)
                .last("LIMIT 1"));
        if (activeBatch != null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前作品已有未完成的章节批次任务");
        }
        String checkType = normalizeCheckType(request.getCheckType());
        List<Chapter> chapters = chapterMapper.selectList(new LambdaQueryWrapper<Chapter>()
                        .eq(Chapter::getProjectId, projectId)
                        .isNotNull(Chapter::getContent)
                        .orderByAsc(Chapter::getChapterNo))
                .stream()
                .filter(this::hasContent)
                .toList();
        if (chapters.isEmpty()) {
            throw new BusinessException(ErrorCode.WORKFLOW_GATE_NOT_MET, "请先生成至少一章正文，再进行检查");
        }

        GenerationBatch batch = new GenerationBatch()
                .setProjectId(projectId)
                .setBatchType("quality_check")
                .setModelConfigId(request.getModelConfigId())
                .setStatus("queued")
                .setRunNo(1)
                .setTotalCount(chapters.size())
                .setPendingCount(chapters.size())
                .setRunningCount(0)
                .setSucceededCount(0)
                .setFailedCount(0)
                .setSkippedCount(0)
                .setQualityCheckedCount(0)
                .setQualityFailedCount(0)
                .setQualityIssueCount(0)
                .setRequestSnapshot(JsonUtils.toJson(Map.of("checkType", checkType)));
        batchMapper.insert(batch);

        List<GenerationBatchItem> items = new ArrayList<>(chapters.size());
        for (Chapter chapter : chapters) {
            GenerationBatchItem item = new GenerationBatchItem()
                    .setBatchId(batch.getId())
                    .setProjectId(projectId)
                    .setChapterId(chapter.getId())
                    .setChapterNo(chapter.getChapterNo())
                    .setItemType("quality_check")
                    .setStatus("pending")
                    .setAttemptCount(0)
                    .setQualityStatus("pending")
                    .setQualityIssueCount(0);
            itemMapper.insert(item);
            items.add(item);
        }
        enqueueBatch(batch);
        return toResponse(batch, items);
    }

    public ChapterGenerationBatchResponse getBatch(Long batchId) {
        GenerationBatch batch = requireBatch(batchId);
        return toResponse(batch, listItems(batchId));
    }

    public ChapterGenerationBatchResponse getQualityCheckBatch(Long batchId) {
        GenerationBatch batch = requireBatch(batchId);
        if (!"quality_check".equals(batch.getBatchType())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "全书检查批次不存在");
        }
        return toResponse(batch, listItems(batchId));
    }

    public List<ChapterGenerationBatchSummaryResponse> listBatches(Long projectId) {
        if (projectMapper.selectById(projectId) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "作品不存在");
        }
        return batchMapper.selectList(new LambdaQueryWrapper<GenerationBatch>()
                        .eq(GenerationBatch::getProjectId, projectId)
                        .orderByDesc(GenerationBatch::getId)
                        .last("LIMIT 50"))
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    public ChapterGenerationBatchResponse getLatestBatch(Long projectId) {
        GenerationBatch batch = batchMapper.selectOne(new LambdaQueryWrapper<GenerationBatch>()
                .eq(GenerationBatch::getProjectId, projectId)
                .orderByDesc(GenerationBatch::getId)
                .last("LIMIT 1"));
        if (batch == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "当前作品还没有章节生成批次");
        }
        return toResponse(batch, listItems(batch.getId()));
    }

    public ChapterGenerationBatchResponse getLatestQualityCheckBatch(Long projectId) {
        GenerationBatch batch = batchMapper.selectOne(new LambdaQueryWrapper<GenerationBatch>()
                .eq(GenerationBatch::getProjectId, projectId)
                .eq(GenerationBatch::getBatchType, "quality_check")
                .orderByDesc(GenerationBatch::getId)
                .last("LIMIT 1"));
        if (batch == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "当前作品还没有全书检查批次");
        }
        return toResponse(batch, listItems(batch.getId()));
    }

    @Transactional
    public ChapterGenerationBatchResponse cancelBatch(Long batchId) {
        GenerationBatch batch = requireBatch(batchId);
        if (TERMINAL_STATUSES.contains(batch.getStatus())) {
            return toResponse(batch, listItems(batchId));
        }
        if ("running".equals(batch.getStatus())) {
            batch.setStatus("cancel_requested");
            batchMapper.updateById(batch);
        } else {
            cancelRemainingItems(batch);
        }
        return toResponse(requireBatch(batchId), listItems(batchId));
    }

    @Transactional
    public ChapterGenerationBatchResponse pauseBatch(Long batchId) {
        GenerationBatch batch = requireBatch(batchId);
        if ("queued".equals(batch.getStatus()) || "running".equals(batch.getStatus())) {
            batch.setStatus("paused");
            batchMapper.updateById(batch);
        }
        return toResponse(batch, listItems(batchId));
    }

    @Transactional
    public ChapterGenerationBatchResponse resumeBatch(Long batchId) {
        GenerationBatch batch = requireBatch(batchId);
        if (!"paused".equals(batch.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "只有暂停中的批次可以继续");
        }
        batch.setStatus("queued")
                .setRunNo(value(batch.getRunNo()) + 1)
                .setFinishedAt(null)
                .setErrorMessage(null);
        batchMapper.updateById(batch);
        enqueueBatch(batch);
        return toResponse(batch, listItems(batchId));
    }

    @Transactional
    public ChapterGenerationBatchResponse retryFailed(Long batchId) {
        GenerationBatch batch = requireBatch(batchId);
        if (!Set.of("failed", "partial_failed").contains(batch.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "只有已结束的失败批次可以重试");
        }
        List<GenerationBatchItem> items = listItems(batchId);
        List<GenerationBatchItem> failedItems = items.stream()
                .filter(item -> "failed".equals(item.getStatus()))
                .toList();
        if (failedItems.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前批次没有失败条目");
        }
        for (GenerationBatchItem item : failedItems) {
            item.setStatus("pending")
                    .setGenerationJobId(null)
                    .setQualityStatus("pending")
                    .setQualityIssueCount(0)
                    .setQualityReport(null)
                    .setQualityErrorMessage(null)
                    .setErrorMessage(null)
                    .setStartedAt(null)
                    .setFinishedAt(null);
            itemMapper.updateById(item);
        }
        batch.setStatus("queued")
                .setRunNo(value(batch.getRunNo()) + 1)
                .setFinishedAt(null)
                .setErrorMessage(null);
        batchMapper.updateById(batch);
        refreshCounts(batchId);
        enqueueBatch(batch);
        return getBatch(batchId);
    }

    void processBatch(Long batchId) {
        processQueuedBatch(batchId, () -> {});
    }

    public String processQueuedBatch(Long batchId, Runnable heartbeat) {
        GenerationBatch batch = batchMapper.selectById(batchId);
        if (batch == null || TERMINAL_STATUSES.contains(batch.getStatus()) || "paused".equals(batch.getStatus())) {
            return batch == null ? "missing" : batch.getStatus();
        }
        if ("cancel_requested".equals(batch.getStatus())) {
            cancelRemainingItems(batch);
            return "cancelled";
        }
        resetRunningItems(batchId);
        batch.setStatus("running");
        if (batch.getStartedAt() == null) {
            batch.setStartedAt(LocalDateTime.now());
        }
        batchMapper.updateById(batch);

        for (GenerationBatchItem item : listItems(batchId)) {
            heartbeat.run();
            GenerationBatch currentBatch = batchMapper.selectById(batchId);
            if (currentBatch == null || "paused".equals(currentBatch.getStatus())) {
                return currentBatch == null ? "missing" : "paused";
            }
            if ("cancel_requested".equals(currentBatch.getStatus())) {
                cancelRemainingItems(currentBatch);
                return "cancelled";
            }
            GenerationBatchItem currentItem = itemMapper.selectById(item.getId());
            if (currentItem == null || !"pending".equals(currentItem.getStatus())) {
                continue;
            }
            executeItem(currentBatch, currentItem);
            heartbeat.run();
            refreshCounts(batchId);
        }
        finishBatch(batchId);
        GenerationBatch finished = batchMapper.selectById(batchId);
        return finished == null ? "missing" : finished.getStatus();
    }

    private void executeItem(GenerationBatch batch, GenerationBatchItem item) {
        item.setStatus("running")
                .setAttemptCount(value(item.getAttemptCount()) + 1)
                .setQualityStatus("pending")
                .setQualityIssueCount(0)
                .setQualityReport(null)
                .setQualityErrorMessage(null)
                .setStartedAt(LocalDateTime.now())
                .setFinishedAt(null)
                .setErrorMessage(null);
        itemMapper.updateById(item);
        try {
            if ("quality_check".equals(batch.getBatchType())) {
                executeQualityCheckItem(batch, item);
            } else {
                ChapterGenerateRequest request = new ChapterGenerateRequest();
                request.setProjectId(batch.getProjectId());
                request.setChapterId(item.getChapterId());
                request.setChapterNo(item.getChapterNo());
                request.setModelConfigId(batch.getModelConfigId());
                request.setRevisionAdvice(batchInstruction(batch));
                ChapterGenerationResult result = chapterService.generateChapterForBatch(request);
                item.setStatus("succeeded")
                        .setGenerationJobId(result.getGenerationJobId())
                        .setFinishedAt(LocalDateTime.now());
                applyQualityResult(item, result.getQualityCheck());
            }
        } catch (RuntimeException ex) {
            item.setStatus("failed")
                    .setQualityStatus("quality_check".equals(batch.getBatchType()) ? "failed" : "not_run")
                    .setQualityErrorMessage("quality_check".equals(batch.getBatchType()) ? errorMessage(ex) : null)
                    .setErrorMessage(errorMessage(ex))
                    .setFinishedAt(LocalDateTime.now());
            log.error("Chapter batch item failed. batchId={}, chapterNo={}", batch.getId(), item.getChapterNo(), ex);
        }
        itemMapper.updateById(item);
    }

    private void executeQualityCheckItem(GenerationBatch batch, GenerationBatchItem item) {
        CheckRequest request = new CheckRequest();
        request.setProjectId(batch.getProjectId());
        request.setChapterId(item.getChapterId());
        request.setModelConfigId(batch.getModelConfigId());
        request.setCheckType(text(JsonUtils.toMap(batch.getRequestSnapshot()).get("checkType")));
        CheckResponse report = checkService.runCheck(request);
        item.setStatus("succeeded")
                .setQualityStatus("completed")
                .setQualityIssueCount(value(report.getIssueCount()))
                .setQualityReport(JsonUtils.toJson(report))
                .setFinishedAt(LocalDateTime.now());
    }

    private void finishBatch(Long batchId) {
        GenerationBatch batch = requireBatch(batchId);
        refreshCounts(batchId);
        batch = requireBatch(batchId);
        if ("paused".equals(batch.getStatus())) {
            return;
        }
        if ("cancel_requested".equals(batch.getStatus())) {
            cancelRemainingItems(batch);
            return;
        }
        if (value(batch.getPendingCount()) > 0 || value(batch.getRunningCount()) > 0) {
            return;
        }
        String status = value(batch.getFailedCount()) == 0
                ? "completed"
                : value(batch.getSucceededCount()) == 0 ? "failed" : "partial_failed";
        batch.setStatus(status).setFinishedAt(LocalDateTime.now());
        batchMapper.updateById(batch);
    }

    private void cancelRemainingItems(GenerationBatch batch) {
        for (GenerationBatchItem item : listItems(batch.getId())) {
            if ("pending".equals(item.getStatus()) || "running".equals(item.getStatus())) {
                item.setStatus("cancelled").setFinishedAt(LocalDateTime.now());
                itemMapper.updateById(item);
            }
        }
        batch.setStatus("cancelled").setFinishedAt(LocalDateTime.now());
        batchMapper.updateById(batch);
        refreshCounts(batch.getId());
    }

    private void refreshCounts(Long batchId) {
        List<GenerationBatchItem> items = listItems(batchId);
        GenerationBatch batch = requireBatch(batchId);
        batch.setPendingCount(count(items, "pending"))
                .setRunningCount(count(items, "running"))
                .setSucceededCount(count(items, "succeeded"))
                .setFailedCount(count(items, "failed"))
                .setSkippedCount(count(items, "skipped"))
                .setQualityCheckedCount(countQuality(items, "completed"))
                .setQualityFailedCount(countQuality(items, "failed"))
                .setQualityIssueCount(items.stream()
                        .mapToInt(item -> value(item.getQualityIssueCount()))
                        .sum());
        batchMapper.updateById(batch);
    }

    private int count(List<GenerationBatchItem> items, String status) {
        return (int) items.stream().filter(item -> status.equals(item.getStatus())).count();
    }

    private int countQuality(List<GenerationBatchItem> items, String status) {
        return (int) items.stream().filter(item -> status.equals(item.getQualityStatus())).count();
    }

    private void applyQualityResult(GenerationBatchItem item, ChapterQualityCheckResult qualityCheck) {
        if (qualityCheck == null) {
            item.setQualityStatus("not_run");
            return;
        }
        item.setQualityStatus(qualityCheck.getStatus())
                .setQualityIssueCount(value(qualityCheck.getIssueCount()))
                .setQualityReport(qualityCheck.getReport() == null ? null : JsonUtils.toJson(qualityCheck.getReport()))
                .setQualityErrorMessage(qualityCheck.getErrorMessage());
    }

    private void validateChapterRange(List<Chapter> chapters, int startChapterNo, int count) {
        if (chapters.size() != count) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "请求范围内存在尚未创建大纲的章节");
        }
        for (int index = 0; index < chapters.size(); index++) {
            if (!Integer.valueOf(startChapterNo + index).equals(chapters.get(index).getChapterNo())) {
                throw new BusinessException(ErrorCode.PARAMETER_ERROR, "请求范围内的章节编号不连续");
            }
        }
    }

    private boolean hasContent(Chapter chapter) {
        return chapter.getContent() != null && !chapter.getContent().isBlank();
    }

    private Map<String, Object> requestSnapshot(ChapterGenerationBatchCreateRequest request) {
        return Map.of(
                "startChapterNo", request.getStartChapterNo(),
                "count", request.getCount(),
                "skipExistingContent", request.getSkipExistingContent(),
                "instruction", text(request.getInstruction()));
    }

    private String batchInstruction(GenerationBatch batch) {
        return text(JsonUtils.toMap(batch.getRequestSnapshot()).get("instruction"));
    }

    private GenerationBatch requireBatch(Long batchId) {
        GenerationBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "章节批次不存在");
        }
        return batch;
    }

    private List<GenerationBatchItem> listItems(Long batchId) {
        return itemMapper.selectList(new LambdaQueryWrapper<GenerationBatchItem>()
                .eq(GenerationBatchItem::getBatchId, batchId)
                .orderByAsc(GenerationBatchItem::getChapterNo));
    }

    private ChapterGenerationBatchResponse toResponse(
            GenerationBatch batch,
            List<GenerationBatchItem> items) {
        List<ChapterGenerationBatchItemResponse> itemResponses = items.stream()
                .sorted(Comparator.comparing(GenerationBatchItem::getChapterNo))
                .map(item -> ChapterGenerationBatchItemResponse.builder()
                        .id(item.getId())
                        .chapterId(item.getChapterId())
                        .chapterNo(item.getChapterNo())
                        .status(item.getStatus())
                        .attemptCount(item.getAttemptCount())
                        .generationJobId(item.getGenerationJobId())
                        .qualityStatus(item.getQualityStatus())
                        .qualityIssueCount(item.getQualityIssueCount())
                        .qualityReport(JsonUtils.toObject(item.getQualityReport(), CheckResponse.class))
                        .qualityErrorMessage(item.getQualityErrorMessage())
                        .errorMessage(item.getErrorMessage())
                        .startedAt(item.getStartedAt())
                        .finishedAt(item.getFinishedAt())
                        .build())
                .toList();
        return ChapterGenerationBatchResponse.builder()
                .batchId(batch.getId())
                .projectId(batch.getProjectId())
                .batchType(batch.getBatchType())
                .modelConfigId(batch.getModelConfigId())
                .status(batch.getStatus())
                .totalCount(batch.getTotalCount())
                .pendingCount(batch.getPendingCount())
                .runningCount(batch.getRunningCount())
                .succeededCount(batch.getSucceededCount())
                .failedCount(batch.getFailedCount())
                .skippedCount(batch.getSkippedCount())
                .qualityCheckedCount(batch.getQualityCheckedCount())
                .qualityFailedCount(batch.getQualityFailedCount())
                .qualityIssueCount(batch.getQualityIssueCount())
                .errorMessage(batch.getErrorMessage())
                .createdAt(batch.getCreatedAt())
                .startedAt(batch.getStartedAt())
                .finishedAt(batch.getFinishedAt())
                .items(itemResponses)
                .build();
    }

    private ChapterGenerationBatchSummaryResponse toSummaryResponse(GenerationBatch batch) {
        return ChapterGenerationBatchSummaryResponse.builder()
                .batchId(batch.getId())
                .projectId(batch.getProjectId())
                .batchType(batch.getBatchType())
                .modelConfigId(batch.getModelConfigId())
                .status(batch.getStatus())
                .totalCount(batch.getTotalCount())
                .pendingCount(batch.getPendingCount())
                .runningCount(batch.getRunningCount())
                .succeededCount(batch.getSucceededCount())
                .failedCount(batch.getFailedCount())
                .skippedCount(batch.getSkippedCount())
                .qualityCheckedCount(batch.getQualityCheckedCount())
                .qualityFailedCount(batch.getQualityFailedCount())
                .qualityIssueCount(batch.getQualityIssueCount())
                .errorMessage(batch.getErrorMessage())
                .createdAt(batch.getCreatedAt())
                .startedAt(batch.getStartedAt())
                .finishedAt(batch.getFinishedAt())
                .build();
    }

    private void enqueueBatch(GenerationBatch batch) {
        String jobType = "quality_check".equals(batch.getBatchType())
                ? "quality_check_batch"
                : "chapter_generation_batch";
        generationJobService.enqueueJob(
                batch.getProjectId(),
                jobType,
                "generation_batch",
                batch.getId(),
                batch.getModelConfigId(),
                Map.of("batchId", batch.getId(), "runNo", batch.getRunNo()),
                batch.getId() + ":" + batch.getRunNo(),
                0,
                LocalDateTime.now());
    }

    private String normalizeCheckType(String checkType) {
        String normalized = text(checkType).toLowerCase();
        if (!Set.of("all", "continuity", "style").contains(normalized)) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "检查类型只支持 all、continuity 或 style");
        }
        return normalized;
    }

    private void resetRunningItems(Long batchId) {
        for (GenerationBatchItem item : listItems(batchId)) {
            if ("running".equals(item.getStatus())) {
                item.setStatus("pending")
                        .setQualityStatus("pending")
                        .setQualityIssueCount(0)
                        .setQualityReport(null)
                        .setQualityErrorMessage(null)
                        .setStartedAt(null);
                itemMapper.updateById(item);
            }
        }
        refreshCounts(batchId);
    }

    @Transactional
    public void markQueueFailure(Long batchId, String errorMessage) {
        GenerationBatch batch = batchMapper.selectById(batchId);
        if (batch == null || TERMINAL_STATUSES.contains(batch.getStatus()) || "paused".equals(batch.getStatus())) {
            return;
        }
        for (GenerationBatchItem item : listItems(batchId)) {
            if ("pending".equals(item.getStatus()) || "running".equals(item.getStatus())) {
                item.setStatus("failed")
                        .setErrorMessage(errorMessage)
                        .setFinishedAt(LocalDateTime.now());
                itemMapper.updateById(item);
            }
        }
        batch.setStatus("failed")
                .setErrorMessage(errorMessage)
                .setFinishedAt(LocalDateTime.now());
        batchMapper.updateById(batch);
        refreshCounts(batchId);
        log.error("Chapter generation batch queue failed. batchId={}, error={}", batchId, errorMessage);
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String errorMessage(Throwable error) {
        return error.getMessage() == null || error.getMessage().isBlank()
                ? error.getClass().getSimpleName()
                : error.getMessage();
    }
}
