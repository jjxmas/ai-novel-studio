package com.jjxmas.ainovelstudio.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.jjxmas.ainovelstudio.ai.AiGenerateResult;
import com.jjxmas.ainovelstudio.ai.AiOrchestratorService;
import com.jjxmas.ainovelstudio.common.exception.BusinessException;
import com.jjxmas.ainovelstudio.common.exception.ErrorCode;
import com.jjxmas.ainovelstudio.common.util.JsonUtils;
import com.jjxmas.ainovelstudio.pojo.entity.Chapter;
import com.jjxmas.ainovelstudio.mapper.ChapterMapper;
import com.jjxmas.ainovelstudio.service.GenerationJobService;
import com.jjxmas.ainovelstudio.pojo.entity.ChapterSummary;
import com.jjxmas.ainovelstudio.pojo.entity.StoryMemory;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterSummaryResponse;
import com.jjxmas.ainovelstudio.pojo.dto.ProjectMemoryResponse;
import com.jjxmas.ainovelstudio.pojo.dto.StoryMemoryResponse;
import com.jjxmas.ainovelstudio.mapper.ChapterSummaryMapper;
import com.jjxmas.ainovelstudio.mapper.StoryMemoryMapper;
import com.jjxmas.ainovelstudio.service.ChapterMemoryService;
import com.jjxmas.ainovelstudio.pojo.entity.Outline;
import com.jjxmas.ainovelstudio.mapper.OutlineMapper;
import com.jjxmas.ainovelstudio.pojo.entity.Project;
import com.jjxmas.ainovelstudio.mapper.ProjectMapper;
import com.jjxmas.ainovelstudio.pojo.entity.SettingLibrary;
import com.jjxmas.ainovelstudio.mapper.SettingLibraryMapper;
import com.jjxmas.ainovelstudio.service.VersionService;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
/**
 * 章节记忆服务实现，负责章节摘要、近期记忆、中层记忆和全局记忆维护。
 */
public class ChapterMemoryServiceImpl implements ChapterMemoryService {

    private static final int RECENT_WINDOW_SIZE = 6;
    private static final int MIDDLE_COMPRESSION_SIZE = 8;

    private final ChapterSummaryMapper chapterSummaryMapper;
    private final ChapterMapper chapterMapper;
    private final StoryMemoryMapper storyMemoryMapper;
    private final ProjectMapper projectMapper;
    private final SettingLibraryMapper settingLibraryMapper;
    private final OutlineMapper outlineMapper;
    private final AiOrchestratorService aiOrchestratorService;
    private final GenerationJobService generationJobService;
    private final VersionService versionService;

    /**
     * 注入记忆流程所需的 Mapper、AI 编排、任务和版本服务。
     */
    public ChapterMemoryServiceImpl(
            ChapterSummaryMapper chapterSummaryMapper,
            ChapterMapper chapterMapper,
            StoryMemoryMapper storyMemoryMapper,
            ProjectMapper projectMapper,
            SettingLibraryMapper settingLibraryMapper,
            OutlineMapper outlineMapper,
            AiOrchestratorService aiOrchestratorService,
            GenerationJobService generationJobService,
            VersionService versionService) {
        this.chapterSummaryMapper = chapterSummaryMapper;
        this.chapterMapper = chapterMapper;
        this.storyMemoryMapper = storyMemoryMapper;
        this.projectMapper = projectMapper;
        this.settingLibraryMapper = settingLibraryMapper;
        this.outlineMapper = outlineMapper;
        this.aiOrchestratorService = aiOrchestratorService;
        this.generationJobService = generationJobService;
        this.versionService = versionService;
    }

    /**
     * 构建章节生成上下文，包含项目、设定、大纲和多层记忆。
     */
    @Override
    public Map<String, Object> buildChapterContext(Chapter chapter) {
        Project project = projectMapper.selectById(chapter.getProjectId());
        SettingLibrary settingLibrary = settingLibraryMapper.selectOne(new LambdaQueryWrapper<SettingLibrary>()
                .eq(SettingLibrary::getProjectId, chapter.getProjectId())
                .last("LIMIT 1"));
        Outline outline = outlineMapper.selectOne(new LambdaQueryWrapper<Outline>()
                .eq(Outline::getProjectId, chapter.getProjectId())
                .last("LIMIT 1"));
        List<StoryMemory> highMemories = currentMemories(chapter.getProjectId(), "high", 8);
        List<StoryMemory> middleMemories = currentMemories(chapter.getProjectId(), "middle", 8);
        List<ChapterSummary> recentSummaries = recentSummaries(chapter.getProjectId());
        Chapter previousChapter = previousChapter(chapter);
        ChapterSummary previousSummary = previousChapter == null ? null : chapterSummaryMapper.selectOne(new LambdaQueryWrapper<ChapterSummary>()
                .eq(ChapterSummary::getChapterId, previousChapter.getId())
                .last("LIMIT 1"));
        StoryMemory globalMemory = currentGlobalMemory(chapter.getProjectId());

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("作品信息", projectContext(project));
        context.put("设定库摘要", settingLibrary == null ? "" : blankToEmpty(settingLibrary.getSummary()));
        context.put("全局大纲", outline == null ? "" : blankToEmpty(outline.getContent()));
        context.put("当前章节", Map.of(
                "chapterNo", chapter.getChapterNo() == null ? 0 : chapter.getChapterNo(),
                "title", blankToEmpty(chapter.getTitle()),
                "outline", blankToEmpty(chapter.getOutline()),
                "scenePlan", blankToEmpty(chapter.getScenePlan())));
        context.put("上一章摘要", previousSummary == null ? "" : blankToEmpty(previousSummary.getSummary()));
        context.put("上一章结尾片段", previousChapter == null ? "" : tailText(previousChapter.getContent(), 500));
        context.put("全局总摘要", globalMemory == null ? "" : blankToEmpty(globalMemory.getContent()));
        context.put("高层摘要", highMemories.stream().map(this::memoryText).toList());
        context.put("中层摘要", middleMemories.stream().map(this::memoryText).toList());
        context.put("近窗摘要", recentSummaries.stream().map(this::summaryText).toList());
        return context;
    }

    private Chapter previousChapter(Chapter chapter) {
        if (chapter.getChapterNo() == null || chapter.getChapterNo() <= 1) {
            return null;
        }
        return chapterMapper.selectOne(new LambdaQueryWrapper<Chapter>()
                .eq(Chapter::getProjectId, chapter.getProjectId())
                .lt(Chapter::getChapterNo, chapter.getChapterNo())
                .isNotNull(Chapter::getContent)
                .orderByDesc(Chapter::getChapterNo)
                .last("LIMIT 1"));
    }

    private String tailText(String content, int maxLength) {
        String text = blankToEmpty(content);
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(text.length() - maxLength);
    }

    /**
     * 在章节正文更新后刷新章节摘要和项目分层记忆。
     */
    @Override
    @Transactional
    public void refreshAfterChapterContent(Chapter chapter, Long modelConfigId) {
        ChapterSummary summary = upsertChapterSummary(chapter, modelConfigId);
        List<ChapterSummary> recent = recentSummaries(chapter.getProjectId());
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
                .globalMemory(toStoryMemoryResponse(currentGlobalMemory(projectId)))
                .highMemories(currentMemories(projectId, "high", 8).stream().map(this::toStoryMemoryResponse).toList())
                .middleMemories(currentMemories(projectId, "middle", 8).stream().map(this::toStoryMemoryResponse).toList())
                .recentWindows(currentMemories(projectId, "recent_window", 1).stream().map(this::toStoryMemoryResponse).toList())
                .recentChapterSummaries(recentSummaries(projectId).stream().map(this::toChapterSummaryResponse).toList())
                .build();
    }

    /**
     * 新增或更新指定章节的摘要。
     */
    private ChapterSummary upsertChapterSummary(Chapter chapter, Long modelConfigId) {
        AiGenerateResult result = aiOrchestratorService.summarizeChapter(modelConfigId, blankToEmpty(chapter.getTitle()), blankToEmpty(chapter.getContent()));
        String content = blankToDefault(result.getContent(), "本章已生成正文，摘要暂由系统占位。");
        Map<String, Object> output = Map.of("summary", content, "modelName", blankToEmpty(result.getModelName()));
        Long jobId = generationJobService.recordFinishedJob(
                chapter.getProjectId(),
                "chapter_summary",
                "chapter",
                chapter.getId(),
                modelConfigId,
                Map.of("chapterId", chapter.getId(), "chapterNo", chapter.getChapterNo()),
                output);

        ChapterSummary existing = chapterSummaryMapper.selectOne(new LambdaQueryWrapper<ChapterSummary>()
                .eq(ChapterSummary::getChapterId, chapter.getId())
                .last("LIMIT 1"));
        ChapterSummary summary = existing == null ? new ChapterSummary() : existing;
        summary.setProjectId(chapter.getProjectId())
                .setChapterId(chapter.getId())
                .setChapterNo(chapter.getChapterNo())
                .setSummary(content)
                .setKeyEvents(JsonUtils.toJson(List.of("关键事件见摘要正文")))
                .setCharacterChanges(JsonUtils.toJson(List.of("人物变化见摘要正文")))
                .setLocationChanges(JsonUtils.toJson(List.of("地点变化见摘要正文")))
                .setForeshadowChanges(JsonUtils.toJson(List.of("伏笔变化见摘要正文")))
                .setGenerationJobId(jobId);
        if (existing == null) {
            chapterSummaryMapper.insert(summary);
        } else {
            chapterSummaryMapper.updateById(summary);
        }
        versionService.recordVersion(
                chapter.getProjectId(),
                "chapter_summary",
                summary.getId(),
                Map.of("chapterId", chapter.getId(), "chapterNo", chapter.getChapterNo(), "summary", content),
                "ai_generate",
                "生成单章摘要",
                modelConfigId,
                jobId);
        return summary;
    }

    /**
     * 用最近章节摘要更新近期窗口记忆。
     */
    private void updateRecentWindowMemory(Long projectId, Long modelConfigId, List<ChapterSummary> recent, ChapterSummary latestSummary) {
        String content = recent.stream().map(this::summaryText).collect(Collectors.joining("\n\n"));
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
                .setStartChapterNo(recent.stream().map(ChapterSummary::getChapterNo).min(Integer::compareTo).orElse(latestSummary.getChapterNo()))
                .setEndChapterNo(recent.stream().map(ChapterSummary::getChapterNo).max(Integer::compareTo).orElse(latestSummary.getChapterNo()))
                .setContent(content)
                .setSourceChapterSummaryIds(JsonUtils.toJson(recent.stream().map(ChapterSummary::getId).toList()))
                .setStatus("active")
                .setCurrent(true)
                .setCompressionRound(0);
        storyMemoryMapper.insert(memory);
        versionService.recordVersion(projectId, "story_memory", memory.getId(), memorySnapshot(memory), "ai_generate", "更新近窗摘要", modelConfigId, null);
    }

    /**
     * 将近期章节摘要压缩为一条近期窗口记忆。
     */
    private StoryMemory compressRecentWindow(Long projectId, Long modelConfigId, List<ChapterSummary> recent) {
        String source = recent.stream().map(this::summaryText).collect(Collectors.joining("\n\n"));
        AiGenerateResult result = aiOrchestratorService.compressMemory(modelConfigId, "最近 6 章摘要", source);
        String content = blankToDefault(result.getContent(), "最近 6 章已压缩为中层摘要。");
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
    }

    /**
     * 将满足条件的近期窗口记忆进一步压缩为中层记忆。
     */
    private void compressMiddleMemories(Long projectId, Long modelConfigId) {
        List<StoryMemory> middleMemories = currentMemories(projectId, "middle", MIDDLE_COMPRESSION_SIZE);
        String source = middleMemories.stream().map(this::memoryText).collect(Collectors.joining("\n\n"));
        AiGenerateResult result = aiOrchestratorService.compressMemory(modelConfigId, "8 条中层摘要", source);
        String content = blankToDefault(result.getContent(), "8 条中层摘要已压缩为高层摘要。");
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
        StoryMemory memory = new StoryMemory()
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
        storyMemoryMapper.insert(memory);
        versionService.recordVersion(projectId, "story_memory", memory.getId(), memorySnapshot(memory), "ai_generate", "8 条中层摘要压缩生成高层摘要", modelConfigId, jobId);
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
    }

    /**
     * 查询项目最近若干章摘要。
     */
    private List<ChapterSummary> recentSummaries(Long projectId) {
        Integer compressedUntil = latestCompressedChapterNo(projectId);
        return chapterSummaryMapper.selectList(new LambdaQueryWrapper<ChapterSummary>()
                .eq(ChapterSummary::getProjectId, projectId)
                .gt(compressedUntil != null, ChapterSummary::getChapterNo, compressedUntil)
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
        return storyMemoryMapper.selectList(new LambdaQueryWrapper<StoryMemory>()
                        .eq(StoryMemory::getProjectId, projectId)
                        .in(StoryMemory::getMemoryType, List.of("middle", "high"))
                        .isNotNull(StoryMemory::getEndChapterNo))
                .stream()
                .map(StoryMemory::getEndChapterNo)
                .max(Integer::compareTo)
                .orElse(null);
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
     * 将项目实体转换为上下文 Map。
     */
    private Map<String, Object> projectContext(Project project) {
        if (project == null) {
            return Map.of();
        }
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("标题", blankToEmpty(project.getTitle()));
        context.put("类型", blankToEmpty(project.getGenres()));
        context.put("平台", blankToEmpty(project.getPlatformTarget()));
        context.put("目标字数下限", project.getTargetWordCountMin() == null ? 0 : project.getTargetWordCountMin());
        context.put("目标字数上限", project.getTargetWordCountMax() == null ? 0 : project.getTargetWordCountMax());
        context.put("单章目标字数", project.getTargetChapterWordCount() == null ? 3000 : project.getTargetChapterWordCount());
        context.put("风格偏好", blankToEmpty(project.getStylePreference()));
        return context;
    }

    /**
     * 将故事记忆实体转换为响应对象。
     */
    private StoryMemoryResponse toStoryMemoryResponse(StoryMemory memory) {
        if (memory == null) {
            return null;
        }
        return StoryMemoryResponse.builder()
                .id(memory.getId())
                .memoryType(memory.getMemoryType())
                .memoryKey(memory.getMemoryKey())
                .sequenceNo(memory.getSequenceNo())
                .startChapterNo(memory.getStartChapterNo())
                .endChapterNo(memory.getEndChapterNo())
                .content(memory.getContent())
                .status(memory.getStatus())
                .current(memory.getCurrent())
                .build();
    }

    /**
     * 将章节摘要实体转换为响应对象。
     */
    private ChapterSummaryResponse toChapterSummaryResponse(ChapterSummary summary) {
        return ChapterSummaryResponse.builder()
                .id(summary.getId())
                .chapterId(summary.getChapterId())
                .chapterNo(summary.getChapterNo())
                .summary(summary.getSummary())
                .build();
    }

    /**
     * 将 null 文本转换为空字符串。
     */
    private String blankToEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * 为空白文本提供默认值。
     */
    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
