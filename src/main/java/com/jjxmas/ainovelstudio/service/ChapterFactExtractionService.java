package com.jjxmas.ainovelstudio.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jjxmas.ainovelstudio.ai.AiGenerateResult;
import com.jjxmas.ainovelstudio.ai.AiOrchestratorService;
import com.jjxmas.ainovelstudio.common.util.JsonUtils;
import com.jjxmas.ainovelstudio.mapper.ChapterFactExtractionRunMapper;
import com.jjxmas.ainovelstudio.mapper.ContentVersionMapper;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterFactExtraction;
import com.jjxmas.ainovelstudio.pojo.entity.Chapter;
import com.jjxmas.ainovelstudio.pojo.entity.ChapterFactExtractionRun;
import com.jjxmas.ainovelstudio.pojo.entity.ContentVersion;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ChapterFactExtractionService {

    private final AiOrchestratorService aiOrchestratorService;
    private final GenerationJobService generationJobService;
    private final VersionService versionService;
    private final ChapterFactExtractionRunMapper chapterFactExtractionRunMapper;
    private final ContentVersionMapper contentVersionMapper;
    private final TransactionTemplate transactionTemplate;

    public ChapterFactExtractionService(
            AiOrchestratorService aiOrchestratorService,
            GenerationJobService generationJobService,
            VersionService versionService,
            ChapterFactExtractionRunMapper chapterFactExtractionRunMapper,
            ContentVersionMapper contentVersionMapper,
            TransactionTemplate transactionTemplate) {
        this.aiOrchestratorService = aiOrchestratorService;
        this.generationJobService = generationJobService;
        this.versionService = versionService;
        this.chapterFactExtractionRunMapper = chapterFactExtractionRunMapper;
        this.contentVersionMapper = contentVersionMapper;
        this.transactionTemplate = transactionTemplate;
    }

    public ChapterFactExtraction extractAndStore(Chapter chapter, Long modelConfigId) {
        Long sourceContentVersionId = chapterContentVersionId(chapter);
        ChapterFactExtractionRun existingRun = findExistingRun(chapter.getId(), sourceContentVersionId);
        if (existingRun != null) {
            return normalizeExtraction(existingRun.getNormalizedOutputJson());
        }
        AiGenerateResult result = aiOrchestratorService.extractChapterFacts(
                modelConfigId,
                blankToEmpty(chapter.getTitle()),
                blankToEmpty(chapter.getContent()));
        String rawOutputJson = result == null ? "" : blankToEmpty(result.getContent());
        ChapterFactExtraction normalized = normalizeExtraction(rawOutputJson);
        String status = resolveStatus(rawOutputJson, normalized);

        Map<String, Object> input = new LinkedHashMap<>();
        input.put("chapterId", chapter.getId());
        input.put("chapterNo", chapter.getChapterNo());
        input.put("title", blankToEmpty(chapter.getTitle()));
        input.put("sourceContentVersionId", sourceContentVersionId);

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("status", status);
        output.put("rawOutputJson", rawOutputJson);
        output.put("normalizedOutput", normalized);
        output.put("issues", normalized.getIssues());
        output.put("modelName", result == null ? "" : blankToEmpty(result.getModelName()));

        try {
            return transactionTemplate.execute(statusContext -> {
                Long jobId = generationJobService.recordFinishedJob(
                        chapter.getProjectId(),
                        "chapter_fact_extraction",
                        "chapter",
                        chapter.getId(),
                        modelConfigId,
                        input,
                        output);

                ChapterFactExtractionRun run = new ChapterFactExtractionRun()
                        .setProjectId(chapter.getProjectId())
                        .setChapterId(chapter.getId())
                        .setSourceContentVersionId(sourceContentVersionId)
                        .setModelConfigId(modelConfigId)
                        .setStatus(status)
                        .setRawOutputJson(rawOutputJson)
                        .setNormalizedOutputJson(JsonUtils.toJson(normalized))
                        .setIssuesJson(JsonUtils.toJson(normalized.getIssues()))
                        .setGenerationJobId(jobId);
                chapterFactExtractionRunMapper.insert(run);

                versionService.recordVersion(
                        chapter.getProjectId(),
                        "chapter_fact_extraction_run",
                        run.getId(),
                        extractionRunSnapshot(run, normalized),
                        "ai_generate",
                        "章节事实抽取",
                        modelConfigId,
                        jobId);
                return normalized;
            });
        } catch (DuplicateKeyException duplicate) {
            ChapterFactExtractionRun concurrentRun = findExistingRun(chapter.getId(), sourceContentVersionId);
            if (concurrentRun != null) {
                return normalizeExtraction(concurrentRun.getNormalizedOutputJson());
            }
            throw duplicate;
        }
    }

    private ChapterFactExtractionRun findExistingRun(Long chapterId, Long sourceContentVersionId) {
        if (sourceContentVersionId == null) {
            return null;
        }
        return chapterFactExtractionRunMapper.selectOne(new LambdaQueryWrapper<ChapterFactExtractionRun>()
                .eq(ChapterFactExtractionRun::getChapterId, chapterId)
                .eq(ChapterFactExtractionRun::getSourceContentVersionId, sourceContentVersionId)
                .last("LIMIT 1"));
    }

    private Long chapterContentVersionId(Chapter chapter) {
        ContentVersion latest = contentVersionMapper.selectOne(new LambdaQueryWrapper<ContentVersion>()
                .eq(ContentVersion::getEntityType, "chapter")
                .eq(ContentVersion::getEntityId, chapter.getId())
                .eq(chapter.getLastContentVersionNo() != null,
                        ContentVersion::getVersionNo,
                        chapter.getLastContentVersionNo())
                .orderByDesc(ContentVersion::getVersionNo)
                .last("LIMIT 1"));
        return latest == null ? null : latest.getId();
    }

    private ChapterFactExtraction normalizeExtraction(String rawOutputJson) {
        List<String> issues = new ArrayList<>();
        ChapterFactExtraction parsed = JsonUtils.toObject(rawOutputJson, ChapterFactExtraction.class);
        if (parsed == null) {
            if (rawOutputJson == null || rawOutputJson.isBlank()) {
                issues.add("fact extraction returned empty content");
            } else {
                issues.add("fact extraction output is not valid ChapterFactExtraction JSON");
            }
            return emptyExtraction(issues);
        }
        if (parsed.getIssues() != null) {
            issues.addAll(parsed.getIssues());
        }
        return ChapterFactExtraction.builder()
                .events(defaultList(parsed.getEvents()))
                .stateChanges(defaultList(parsed.getStateChanges()))
                .relationChanges(defaultList(parsed.getRelationChanges()))
                .foreshadowChanges(defaultList(parsed.getForeshadowChanges()))
                .unresolvedThreads(defaultList(parsed.getUnresolvedThreads()))
                .issues(issues)
                .build();
    }

    private String resolveStatus(String rawOutputJson, ChapterFactExtraction normalized) {
        if (rawOutputJson == null || rawOutputJson.isBlank()) {
            return "empty";
        }
        return normalized.getIssues() == null || normalized.getIssues().isEmpty() ? "success" : "partial";
    }

    private Map<String, Object> extractionRunSnapshot(ChapterFactExtractionRun run, ChapterFactExtraction normalized) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("chapterId", run.getChapterId());
        snapshot.put("sourceContentVersionId", run.getSourceContentVersionId());
        snapshot.put("status", blankToEmpty(run.getStatus()));
        snapshot.put("normalizedOutput", normalized);
        snapshot.put("issues", normalized.getIssues());
        return snapshot;
    }

    private ChapterFactExtraction emptyExtraction(List<String> issues) {
        return ChapterFactExtraction.builder()
                .events(List.of())
                .stateChanges(List.of())
                .relationChanges(List.of())
                .foreshadowChanges(List.of())
                .unresolvedThreads(List.of())
                .issues(issues)
                .build();
    }

    private <T> List<T> defaultList(List<T> value) {
        return value == null ? List.of() : value;
    }

    private String blankToEmpty(String value) {
        return value == null ? "" : value;
    }
}
