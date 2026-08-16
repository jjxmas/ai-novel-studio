package com.jjxmas.ainovelstudio.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.jjxmas.ainovelstudio.ai.AiGenerateResult;
import com.jjxmas.ainovelstudio.ai.AiOrchestratorService;
import com.jjxmas.ainovelstudio.common.exception.BusinessException;
import com.jjxmas.ainovelstudio.common.exception.ErrorCode;
import com.jjxmas.ainovelstudio.common.util.JsonUtils;
import com.jjxmas.ainovelstudio.converter.ChapterMemoryConverter;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterFactExtraction;
import com.jjxmas.ainovelstudio.pojo.entity.Chapter;
import com.jjxmas.ainovelstudio.service.GenerationJobService;
import com.jjxmas.ainovelstudio.pojo.entity.ChapterSummary;
import com.jjxmas.ainovelstudio.pojo.entity.ContentVersion;
import com.jjxmas.ainovelstudio.pojo.entity.StoryMemory;
import com.jjxmas.ainovelstudio.pojo.dto.ProjectMemoryResponse;
import com.jjxmas.ainovelstudio.mapper.ChapterSummaryMapper;
import com.jjxmas.ainovelstudio.mapper.ContentVersionMapper;
import com.jjxmas.ainovelstudio.mapper.StoryMemoryMapper;
import com.jjxmas.ainovelstudio.service.ChapterMemoryService;
import com.jjxmas.ainovelstudio.service.ChapterFactExtractionService;
import com.jjxmas.ainovelstudio.service.ForeshadowThreadService;
import com.jjxmas.ainovelstudio.service.StoryFactProjectionService;
import com.jjxmas.ainovelstudio.mapper.ProjectMapper;
import com.jjxmas.ainovelstudio.service.VersionService;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
/**
 * 章节记忆服务实现，负责章节摘要、近期记忆、中层记忆和全局记忆维护。
 */
public class ChapterMemoryServiceImpl implements ChapterMemoryService {

    private static final int RECENT_WINDOW_SIZE = 6;
    private static final int MIDDLE_COMPRESSION_SIZE = 8;

    private final ChapterSummaryMapper chapterSummaryMapper;
    private final ContentVersionMapper contentVersionMapper;
    private final StoryMemoryMapper storyMemoryMapper;
    private final ProjectMapper projectMapper;
    private final AiOrchestratorService aiOrchestratorService;
    private final GenerationJobService generationJobService;
    private final VersionService versionService;
    private final ChapterMemoryConverter chapterMemoryConverter;
    private final ChapterFactExtractionService chapterFactExtractionService;
    private final ForeshadowThreadService foreshadowThreadService;
    private final StoryFactProjectionService storyFactProjectionService;
    private final TransactionTemplate transactionTemplate;

    /**
     * 注入记忆流程所需的 Mapper、AI 编排、任务和版本服务。
     */
    public ChapterMemoryServiceImpl(
            ChapterSummaryMapper chapterSummaryMapper,
            ContentVersionMapper contentVersionMapper,
            StoryMemoryMapper storyMemoryMapper,
            ProjectMapper projectMapper,
            AiOrchestratorService aiOrchestratorService,
            GenerationJobService generationJobService,
            VersionService versionService,
            ChapterMemoryConverter chapterMemoryConverter,
            ChapterFactExtractionService chapterFactExtractionService,
            ForeshadowThreadService foreshadowThreadService,
            StoryFactProjectionService storyFactProjectionService,
            TransactionTemplate transactionTemplate) {
        this.chapterSummaryMapper = chapterSummaryMapper;
        this.contentVersionMapper = contentVersionMapper;
        this.storyMemoryMapper = storyMemoryMapper;
        this.projectMapper = projectMapper;
        this.aiOrchestratorService = aiOrchestratorService;
        this.generationJobService = generationJobService;
        this.versionService = versionService;
        this.chapterMemoryConverter = chapterMemoryConverter;
        this.chapterFactExtractionService = chapterFactExtractionService;
        this.foreshadowThreadService = foreshadowThreadService;
        this.storyFactProjectionService = storyFactProjectionService;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * 在章节正文更新后刷新章节摘要和项目分层记忆。
     */
    @Override
    @CacheEvict(value = "chapterContextMemoryStacks", key = "#chapter.projectId")
    public void refreshAfterChapterContent(Chapter chapter, Long modelConfigId) {
        refreshFactProjection(chapter, modelConfigId);
        refreshNarrativeMemory(chapter, modelConfigId);
    }

    @Override
    public void refreshFactProjection(Chapter chapter, Long modelConfigId) {
        ChapterFactExtraction extraction = chapterFactExtractionService.extractAndStore(chapter, modelConfigId);
        storyFactProjectionService.projectChapterFacts(chapter, extraction);
        foreshadowThreadService.applyFactExtraction(chapter, extraction);
    }

    @Override
    public void clearFactProjection(Chapter chapter) {
        ChapterFactExtraction emptyExtraction = ChapterFactExtraction.builder().build();
        storyFactProjectionService.projectChapterFacts(chapter, emptyExtraction);
        foreshadowThreadService.applyFactExtraction(chapter, emptyExtraction);
    }

    @Override
    @CacheEvict(value = "chapterContextMemoryStacks", key = "#chapter.projectId")
    public void refreshNarrativeMemory(Chapter chapter, Long modelConfigId) {
        ChapterSummary summary = upsertChapterSummary(chapter, modelConfigId);
        List<ChapterSummary> recent = recentSummaries(chapter.getProjectId(), chapter.getChapterNo());
        if (recent.size() >= RECENT_WINDOW_SIZE && !hasCurrentMiddleCovering(chapter.getProjectId(), recent)) {
            StoryMemory middleMemory = compressRecentWindow(chapter.getProjectId(), modelConfigId, recent);
            if (currentMemories(chapter.getProjectId(), "middle", MIDDLE_COMPRESSION_SIZE).size() >= MIDDLE_COMPRESSION_SIZE) {
                compressMiddleMemories(chapter.getProjectId(), modelConfigId);
            } else {
                updateGlobalMemory(chapter.getProjectId(), modelConfigId, middleMemory.getContent());
            }
        } else {
            updateRecentWindowMemory(chapter.getProjectId(), modelConfigId, recent, summary);
        }
    }

    @Override
    @CacheEvict(value = "chapterContextMemoryStacks", key = "#projectId")
    public void resetNarrativeMemory(Long projectId) {
        storyMemoryMapper.delete(new LambdaQueryWrapper<StoryMemory>()
                .eq(StoryMemory::getProjectId, projectId));
    }

    /**
     * 查询项目当前的全局、高层、中层、近期窗口和章节摘要记忆。
     */
    @Override
    public ProjectMemoryResponse getProjectMemory(Long projectId) {
        if (projectMapper.selectById(projectId) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "作品不存在");
        }
        return ProjectMemoryResponse.builder()
                .projectId(projectId)
                .globalMemory(chapterMemoryConverter.toStoryMemoryResponse(currentGlobalMemory(projectId)))
                .highMemories(chapterMemoryConverter.toStoryMemoryResponseList(currentMemories(projectId, "high", 8)))
                .middleMemories(chapterMemoryConverter.toStoryMemoryResponseList(currentMemories(projectId, "middle", 8)))
                .recentWindows(chapterMemoryConverter.toStoryMemoryResponseList(currentMemories(projectId, "recent_window", 1)))
                .recentChapterSummaries(chapterMemoryConverter.toChapterSummaryResponseList(recentSummaries(projectId)))
                .build();
    }

    /**
     * 新增或更新指定章节的摘要。
     */
    private ChapterSummary upsertChapterSummary(Chapter chapter, Long modelConfigId) {
        Long sourceContentVersionId = chapterContentVersionId(chapter);
        ChapterSummary existing = chapterSummaryMapper.selectOne(new LambdaQueryWrapper<ChapterSummary>()
                .eq(ChapterSummary::getChapterId, chapter.getId())
                .last("LIMIT 1"));
        if (existing != null && sourceContentVersionId != null
                && sourceContentVersionId.equals(existing.getSourceContentVersionId())) {
            return existing;
        }
        AiGenerateResult result = aiOrchestratorService.summarizeChapter(modelConfigId, blankToEmpty(chapter.getTitle()), blankToEmpty(chapter.getContent()));
        Map<String, Object> structured = JsonUtils.toMap(result.getContent());
        String content = structured.isEmpty()
                ? blankToDefault(result.getContent(), "本章已生成正文，摘要暂由系统占位。")
                : blankToDefault(textValue(structured.get("summary")), blankToDefault(result.getContent(), "本章摘要为空。"));
        String endingState = textValue(structured.get("endingState"));
        String unresolvedThreads = textListValue(structured.get("unresolvedThreads"));
        if (!endingState.isBlank()) {
            content += "\n章末状态：" + endingState;
        }
        if (!unresolvedThreads.isBlank()) {
            content += "\n未解决事项：" + unresolvedThreads;
        }
        String summaryContent = content;
        Map<String, Object> output = Map.of("summary", summaryContent, "modelName", blankToEmpty(result.getModelName()));
        return transactionTemplate.execute(status -> {
            Long jobId = generationJobService.recordFinishedJob(
                    chapter.getProjectId(),
                    "chapter_summary",
                    "chapter",
                    chapter.getId(),
                    modelConfigId,
                    Map.of("chapterId", chapter.getId(), "chapterNo", chapter.getChapterNo()),
                    output);

            ChapterSummary summary = existing == null ? new ChapterSummary() : existing;
            summary.setProjectId(chapter.getProjectId())
                    .setChapterId(chapter.getId())
                    .setChapterNo(chapter.getChapterNo())
                    .setSummary(summaryContent)
                    .setKeyEvents(jsonListField(structured, "keyEvents", "关键事件见摘要正文"))
                    .setCharacterChanges(jsonListField(structured, "characterChanges", "人物变化见摘要正文"))
                    .setLocationChanges(jsonListField(structured, "locationChanges", "地点变化见摘要正文"))
                    .setForeshadowChanges(jsonListField(structured, "foreshadowChanges", "伏笔变化见摘要正文"))
                    .setSourceContentVersionId(sourceContentVersionId)
                    .setGenerationJobId(jobId);
            if (existing == null) {
                chapterSummaryMapper.insert(summary);
            } else {
                chapterSummaryMapper.updateById(summary);
            }
            int summaryVersionNo = versionService.recordVersion(
                    chapter.getProjectId(),
                    "chapter_summary",
                    summary.getId(),
                    Map.of("chapterId", chapter.getId(), "chapterNo", chapter.getChapterNo(), "summary", summaryContent),
                    "ai_generate",
                    "生成单章摘要",
                    modelConfigId,
                    jobId);
            ContentVersion summaryVersion = contentVersionMapper.selectOne(new LambdaQueryWrapper<ContentVersion>()
                    .eq(ContentVersion::getEntityType, "chapter_summary")
                    .eq(ContentVersion::getEntityId, summary.getId())
                    .eq(ContentVersion::getVersionNo, summaryVersionNo)
                    .last("LIMIT 1"));
            if (summaryVersion != null) {
                summary.setSummaryVersionId(summaryVersion.getId());
                chapterSummaryMapper.updateById(summary);
            }
            return summary;
        });
    }

    private Long chapterContentVersionId(Chapter chapter) {
        ContentVersion contentVersion = contentVersionMapper.selectOne(new LambdaQueryWrapper<ContentVersion>()
                .eq(ContentVersion::getEntityType, "chapter")
                .eq(ContentVersion::getEntityId, chapter.getId())
                .eq(chapter.getLastContentVersionNo() != null,
                        ContentVersion::getVersionNo,
                        chapter.getLastContentVersionNo())
                .orderByDesc(ContentVersion::getVersionNo)
                .last("LIMIT 1"));
        return contentVersion == null ? null : contentVersion.getId();
    }

    /**
     * 用最近章节摘要更新近期窗口记忆。
     */
    private void updateRecentWindowMemory(Long projectId, Long modelConfigId, List<ChapterSummary> recent, ChapterSummary latestSummary) {
        String content = recent.stream().map(this::summaryText).collect(Collectors.joining("\n\n"));
        String sourceSummaryIds = JsonUtils.toJson(recent.stream().map(ChapterSummary::getId).toList());
        Integer startChapterNo = recent.stream()
                .map(ChapterSummary::getChapterNo)
                .min(Integer::compareTo)
                .orElse(latestSummary.getChapterNo());
        Integer endChapterNo = recent.stream()
                .map(ChapterSummary::getChapterNo)
                .max(Integer::compareTo)
                .orElse(latestSummary.getChapterNo());
        StoryMemory current = currentMemories(projectId, "recent_window", 1).stream().findFirst().orElse(null);
        if (current != null
                && Objects.equals(current.getContent(), content)
                && Objects.equals(current.getSourceChapterSummaryIds(), sourceSummaryIds)
                && Objects.equals(current.getStartChapterNo(), startChapterNo)
                && Objects.equals(current.getEndChapterNo(), endChapterNo)) {
            return;
        }
        transactionTemplate.execute(status -> {
            storyMemoryMapper.update(null, new LambdaUpdateWrapper<StoryMemory>()
                    .eq(StoryMemory::getProjectId, projectId)
                    .eq(StoryMemory::getMemoryType, "recent_window")
                    .eq(StoryMemory::getCurrent, true)
                    .set(StoryMemory::getStatus, "superseded")
                    .set(StoryMemory::getCurrent, false));
            StoryMemory memory = new StoryMemory()
                    .setProjectId(projectId)
                    .setMemoryType("recent_window")
                    .setMemoryKey("recent-window")
                    .setSequenceNo(nextSequence(projectId, "recent_window"))
                    .setStartChapterNo(startChapterNo)
                    .setEndChapterNo(endChapterNo)
                    .setContent(content)
                    .setSourceChapterSummaryIds(sourceSummaryIds)
                    .setStatus("active")
                    .setCurrent(true)
                    .setCompressionRound(0);
            storyMemoryMapper.insert(memory);
            versionService.recordVersion(projectId, "story_memory", memory.getId(), memorySnapshot(memory), "ai_generate", "更新近窗摘要", modelConfigId, null);
            return null;
        });
    }

    /**
     * 将近期章节摘要压缩为一条近期窗口记忆。
     */
    private StoryMemory compressRecentWindow(Long projectId, Long modelConfigId, List<ChapterSummary> recent) {
        String source = recent.stream().map(this::summaryText).collect(Collectors.joining("\n\n"));
        AiGenerateResult result = aiOrchestratorService.compressMemory(modelConfigId, "最近 6 章摘要", source);
        String content = blankToDefault(result.getContent(), "最近 6 章已压缩为中层摘要。");
        return transactionTemplate.execute(status -> {
            Long jobId = generationJobService.recordFinishedJob(
                    projectId,
                    "memory_middle_compression",
                    "story_memory",
                    projectId,
                    modelConfigId,
                    Map.of("sourceChapterSummaryIds", recent.stream().map(ChapterSummary::getId).toList()),
                    Map.of("content", content));
            storyMemoryMapper.update(null, new LambdaUpdateWrapper<StoryMemory>()
                    .eq(StoryMemory::getProjectId, projectId)
                    .eq(StoryMemory::getMemoryType, "recent_window")
                    .eq(StoryMemory::getCurrent, true)
                    .set(StoryMemory::getStatus, "compressed")
                    .set(StoryMemory::getCurrent, false));
            int sequenceNo = nextSequence(projectId, "middle");
            StoryMemory memory = new StoryMemory()
                    .setProjectId(projectId)
                    .setMemoryType("middle")
                    .setMemoryKey("middle-" + sequenceNo)
                    .setSequenceNo(sequenceNo)
                    .setStartChapterNo(recent.stream().map(ChapterSummary::getChapterNo).min(Integer::compareTo).orElse(null))
                    .setEndChapterNo(recent.stream().map(ChapterSummary::getChapterNo).max(Integer::compareTo).orElse(null))
                    .setContent(content)
                    .setSourceChapterSummaryIds(JsonUtils.toJson(recent.stream().map(ChapterSummary::getId).toList()))
                    .setStatus("active")
                    .setCurrent(true)
                    .setCompressionRound(1)
                    .setGenerationJobId(jobId);
            storyMemoryMapper.insert(memory);
            versionService.recordVersion(projectId, "story_memory", memory.getId(), memorySnapshot(memory), "ai_generate", "近窗满 6 章，压缩生成中层摘要", modelConfigId, jobId);
            return memory;
        });
    }

    /**
     * 将满足条件的近期窗口记忆进一步压缩为中层记忆。
     */
    private void compressMiddleMemories(Long projectId, Long modelConfigId) {
        List<StoryMemory> middleMemories = currentMemories(projectId, "middle", MIDDLE_COMPRESSION_SIZE);
        String source = middleMemories.stream().map(this::memoryText).collect(Collectors.joining("\n\n"));
        AiGenerateResult result = aiOrchestratorService.compressMemory(modelConfigId, "8 条中层摘要", source);
        String content = blankToDefault(result.getContent(), "8 条中层摘要已压缩为高层摘要。");
        StoryMemory memory = transactionTemplate.execute(status -> {
            Long jobId = generationJobService.recordFinishedJob(
                    projectId,
                    "memory_high_compression",
                    "story_memory",
                    projectId,
                    modelConfigId,
                    Map.of("sourceMemoryIds", middleMemories.stream().map(StoryMemory::getId).toList()),
                    Map.of("content", content));
            storyMemoryMapper.update(null, new LambdaUpdateWrapper<StoryMemory>()
                    .eq(StoryMemory::getProjectId, projectId)
                    .eq(StoryMemory::getMemoryType, "middle")
                    .eq(StoryMemory::getCurrent, true)
                    .set(StoryMemory::getStatus, "compressed")
                    .set(StoryMemory::getCurrent, false));
            int sequenceNo = nextSequence(projectId, "high");
            StoryMemory highMemory = new StoryMemory()
                    .setProjectId(projectId)
                    .setMemoryType("high")
                    .setMemoryKey("high-" + sequenceNo)
                    .setSequenceNo(sequenceNo)
                    .setStartChapterNo(middleMemories.stream().map(StoryMemory::getStartChapterNo).min(Comparator.nullsLast(Integer::compareTo)).orElse(null))
                    .setEndChapterNo(middleMemories.stream().map(StoryMemory::getEndChapterNo).max(Comparator.nullsFirst(Integer::compareTo)).orElse(null))
                    .setContent(content)
                    .setSourceMemoryIds(JsonUtils.toJson(middleMemories.stream().map(StoryMemory::getId).toList()))
                    .setStatus("active")
                    .setCurrent(true)
                    .setCompressionRound(2)
                    .setGenerationJobId(jobId);
            storyMemoryMapper.insert(highMemory);
            versionService.recordVersion(projectId, "story_memory", highMemory.getId(), memorySnapshot(highMemory), "ai_generate", "8 条中层摘要压缩生成高层摘要", modelConfigId, jobId);
            return highMemory;
        });
        updateGlobalMemory(projectId, modelConfigId, memory.getContent());
    }

    /**
     * 根据新的中层记忆更新项目全局记忆。
     */
    private void updateGlobalMemory(Long projectId, Long modelConfigId, String newMemory) {
        StoryMemory oldGlobal = currentGlobalMemory(projectId);
        AiGenerateResult result = aiOrchestratorService.updateGlobalMemory(
                modelConfigId,
                oldGlobal == null ? "" : oldGlobal.getContent(),
                newMemory);
        String content = blankToDefault(result.getContent(), newMemory);
        transactionTemplate.execute(status -> {
            Long jobId = generationJobService.recordFinishedJob(
                    projectId,
                    "global_memory_update",
                    "story_memory",
                    projectId,
                    modelConfigId,
                    Map.of("oldGlobalMemoryId", oldGlobal == null ? 0 : oldGlobal.getId()),
                    Map.of("content", content));
            storyMemoryMapper.update(null, new LambdaUpdateWrapper<StoryMemory>()
                    .eq(StoryMemory::getProjectId, projectId)
                    .eq(StoryMemory::getMemoryType, "global")
                    .eq(StoryMemory::getCurrent, true)
                    .set(StoryMemory::getStatus, "superseded")
                    .set(StoryMemory::getCurrent, false));
            StoryMemory global = new StoryMemory()
                    .setProjectId(projectId)
                    .setMemoryType("global")
                    .setMemoryKey("global")
                    .setSequenceNo(nextSequence(projectId, "global"))
                    .setContent(content)
                    .setSourceMemoryIds(oldGlobal == null ? null : JsonUtils.toJson(List.of(oldGlobal.getId())))
                    .setStatus("active")
                    .setCurrent(true)
                    .setCompressionRound(3)
                    .setGenerationJobId(jobId);
            storyMemoryMapper.insert(global);
            versionService.recordVersion(projectId, "story_memory", global.getId(), memorySnapshot(global), "ai_generate", "同步更新全局总摘要", modelConfigId, jobId);
            return null;
        });
    }

    /**
     * 查询项目最近若干章摘要。
     */
    private List<ChapterSummary> recentSummaries(Long projectId) {
        return recentSummaries(projectId, null);
    }

    private List<ChapterSummary> recentSummaries(Long projectId, Integer throughChapterNo) {
        Integer compressedUntil = latestCompressedChapterNo(projectId);
        return chapterSummaryMapper.selectList(new LambdaQueryWrapper<ChapterSummary>()
                .eq(ChapterSummary::getProjectId, projectId)
                .gt(compressedUntil != null, ChapterSummary::getChapterNo, compressedUntil)
                .le(throughChapterNo != null, ChapterSummary::getChapterNo, throughChapterNo)
                .orderByAsc(ChapterSummary::getChapterNo)
                .last("LIMIT " + RECENT_WINDOW_SIZE));
    }

    /**
     * 查询项目指定类型的当前有效记忆。
     */
    private List<StoryMemory> currentMemories(Long projectId, String memoryType, int limit) {
        return storyMemoryMapper.selectList(new LambdaQueryWrapper<StoryMemory>()
                .eq(StoryMemory::getProjectId, projectId)
                .eq(StoryMemory::getMemoryType, memoryType)
                .eq(StoryMemory::getCurrent, true)
                .orderByAsc(StoryMemory::getSequenceNo)
                .last("LIMIT " + limit));
    }

    /**
     * 查询项目当前全局记忆。
     */
    private StoryMemory currentGlobalMemory(Long projectId) {
        return storyMemoryMapper.selectOne(new LambdaQueryWrapper<StoryMemory>()
                .eq(StoryMemory::getProjectId, projectId)
                .eq(StoryMemory::getMemoryType, "global")
                .eq(StoryMemory::getCurrent, true)
                .last("LIMIT 1"));
    }

    /**
     * 查询项目已压缩到的最新章节号。
     */
    private Integer latestCompressedChapterNo(Long projectId) {
        StoryMemory latest = storyMemoryMapper.selectOne(new LambdaQueryWrapper<StoryMemory>()
                .eq(StoryMemory::getProjectId, projectId)
                .in(StoryMemory::getMemoryType, List.of("middle", "high"))
                .isNotNull(StoryMemory::getEndChapterNo)
                .orderByDesc(StoryMemory::getEndChapterNo)
                .last("LIMIT 1"));
        return latest == null ? null : latest.getEndChapterNo();
    }

    /**
     * 判断当前中层记忆是否已经覆盖最近章节范围。
     */
    private boolean hasCurrentMiddleCovering(Long projectId, List<ChapterSummary> recent) {
        Integer startChapterNo = recent.stream().map(ChapterSummary::getChapterNo).min(Integer::compareTo).orElse(null);
        Integer endChapterNo = recent.stream().map(ChapterSummary::getChapterNo).max(Integer::compareTo).orElse(null);
        if (startChapterNo == null || endChapterNo == null) {
            return false;
        }
        Long count = storyMemoryMapper.selectCount(new LambdaQueryWrapper<StoryMemory>()
                .eq(StoryMemory::getProjectId, projectId)
                .eq(StoryMemory::getMemoryType, "middle")
                .eq(StoryMemory::getCurrent, true)
                .eq(StoryMemory::getStartChapterNo, startChapterNo)
                .eq(StoryMemory::getEndChapterNo, endChapterNo));
        return count != null && count > 0;
    }

    /**
     * 计算指定项目和记忆类型的下一个序号。
     */
    private int nextSequence(Long projectId, String memoryType) {
        StoryMemory latest = storyMemoryMapper.selectOne(new LambdaQueryWrapper<StoryMemory>()
                .eq(StoryMemory::getProjectId, projectId)
                .eq(StoryMemory::getMemoryType, memoryType)
                .orderByDesc(StoryMemory::getSequenceNo)
                .last("LIMIT 1"));
        return latest == null ? 1 : latest.getSequenceNo() + 1;
    }

    /**
     * 读取章节摘要文本并兜底为空字符串。
     */
    private String summaryText(ChapterSummary summary) {
        return "第 %d 章：%s".formatted(summary.getChapterNo(), summary.getSummary());
    }

    /**
     * 读取故事记忆文本并兜底为空字符串。
     */
    private String memoryText(StoryMemory memory) {
        return "%s（第 %s-%s 章）：%s".formatted(
                memory.getMemoryKey(),
                memory.getStartChapterNo() == null ? "?" : memory.getStartChapterNo(),
                memory.getEndChapterNo() == null ? "?" : memory.getEndChapterNo(),
                memory.getContent());
    }

    /**
     * 将故事记忆转换为版本快照。
     */
    private Map<String, Object> memorySnapshot(StoryMemory memory) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("memoryType", blankToEmpty(memory.getMemoryType()));
        snapshot.put("memoryKey", blankToEmpty(memory.getMemoryKey()));
        snapshot.put("startChapterNo", memory.getStartChapterNo() == null ? 0 : memory.getStartChapterNo());
        snapshot.put("endChapterNo", memory.getEndChapterNo() == null ? 0 : memory.getEndChapterNo());
        snapshot.put("content", blankToEmpty(memory.getContent()));
        return snapshot;
    }

    /**
     * 将 null 文本转换为空字符串。
     */
    private String blankToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String textValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String textListValue(Object value) {
        if (!(value instanceof List<?> list)) {
            return textValue(value);
        }
        return list.stream().map(this::textValue).filter(text -> !text.isBlank()).collect(Collectors.joining("；"));
    }

    private String jsonListField(Map<String, Object> source, String key, String fallback) {
        Object value = source.get(key);
        if (value instanceof List<?> list) {
            return JsonUtils.toJson(list);
        }
        return JsonUtils.toJson(List.of(fallback));
    }

    /**
     * 为空白文本提供默认值。
     */
    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
