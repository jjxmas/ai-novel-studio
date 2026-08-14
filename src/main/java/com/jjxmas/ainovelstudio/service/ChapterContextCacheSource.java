package com.jjxmas.ainovelstudio.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jjxmas.ainovelstudio.mapper.ChapterSummaryMapper;
import com.jjxmas.ainovelstudio.mapper.OutlineMapper;
import com.jjxmas.ainovelstudio.mapper.ProjectMapper;
import com.jjxmas.ainovelstudio.mapper.SettingLibraryMapper;
import com.jjxmas.ainovelstudio.mapper.StoryMemoryMapper;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterContext;
import com.jjxmas.ainovelstudio.pojo.entity.ChapterSummary;
import com.jjxmas.ainovelstudio.pojo.entity.Outline;
import com.jjxmas.ainovelstudio.pojo.entity.Project;
import com.jjxmas.ainovelstudio.pojo.entity.SettingLibrary;
import com.jjxmas.ainovelstudio.pojo.entity.StoryMemory;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class ChapterContextCacheSource {

    private static final int RECENT_WINDOW_SIZE = 6;

    private final ProjectMapper projectMapper;
    private final SettingLibraryMapper settingLibraryMapper;
    private final OutlineMapper outlineMapper;
    private final StoryMemoryMapper storyMemoryMapper;
    private final ChapterSummaryMapper chapterSummaryMapper;

    public ChapterContextCacheSource(
            ProjectMapper projectMapper,
            SettingLibraryMapper settingLibraryMapper,
            OutlineMapper outlineMapper,
            StoryMemoryMapper storyMemoryMapper,
            ChapterSummaryMapper chapterSummaryMapper) {
        this.projectMapper = projectMapper;
        this.settingLibraryMapper = settingLibraryMapper;
        this.outlineMapper = outlineMapper;
        this.storyMemoryMapper = storyMemoryMapper;
        this.chapterSummaryMapper = chapterSummaryMapper;
    }

    @Cacheable(value = "chapterContextProfiles", key = "#projectId")
    public ChapterContext.ProjectProfile projectProfile(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            return ChapterContext.ProjectProfile.builder()
                    .projectId(null)
                    .title("")
                    .genres("")
                    .platformTarget("")
                    .targetWordCountMin(0)
                    .targetWordCountMax(0)
                    .targetChapterWordCount(3000)
                    .stylePreference("")
                    .build();
        }
        return ChapterContext.ProjectProfile.builder()
                .projectId(project.getId())
                .title(blankToEmpty(project.getTitle()))
                .genres(joinGenres(project.getGenres()))
                .platformTarget(blankToEmpty(project.getPlatformTarget()))
                .targetWordCountMin(project.getTargetWordCountMin() == null ? 0 : project.getTargetWordCountMin())
                .targetWordCountMax(project.getTargetWordCountMax() == null ? 0 : project.getTargetWordCountMax())
                .targetChapterWordCount(project.getTargetChapterWordCount() == null ? 3000 : project.getTargetChapterWordCount())
                .stylePreference(blankToEmpty(project.getStylePreference()))
                .build();
    }

    @Cacheable(value = "chapterContextSettings", key = "#projectId")
    public ChapterContext.ImmutableSetting immutableSetting(Long projectId) {
        SettingLibrary settingLibrary = settingLibraryMapper.selectOne(new LambdaQueryWrapper<SettingLibrary>()
                .eq(SettingLibrary::getProjectId, projectId)
                .last("LIMIT 1"));
        return ChapterContext.ImmutableSetting.builder()
                .settingSummary(settingLibrary == null ? "" : blankToEmpty(settingLibrary.getSummary()))
                .settingOverview(settingLibrary == null ? "" : blankToEmpty(settingLibrary.getOverview()))
                .build();
    }

    @Cacheable(value = "chapterContextOutlines", key = "#projectId")
    public ChapterContext.StoryPlan storyPlan(Long projectId) {
        Outline outline = outlineMapper.selectOne(new LambdaQueryWrapper<Outline>()
                .eq(Outline::getProjectId, projectId)
                .last("LIMIT 1"));
        return ChapterContext.StoryPlan.builder()
                .globalOutline(outline == null ? "" : blankToEmpty(outline.getContent()))
                .build();
    }

    @Cacheable(value = "chapterContextMemoryStacks", key = "#projectId")
    public ChapterContext.MemoryStack memoryStack(Long projectId) {
        StoryMemory globalMemory = currentGlobalMemory(projectId);
        List<StoryMemory> highMemories = currentMemories(projectId, "high", 8);
        List<StoryMemory> middleMemories = currentMemories(projectId, "middle", 8);
        List<ChapterSummary> recentSummaries = recentSummaries(projectId);
        return ChapterContext.MemoryStack.builder()
                .globalMemory(globalMemory == null ? "" : blankToEmpty(globalMemory.getContent()))
                .highMemories(highMemories.stream().map(this::memoryText).toList())
                .middleMemories(middleMemories.stream().map(this::memoryText).toList())
                .recentSummaries(recentSummaries.stream().map(this::summaryText).toList())
                .build();
    }

    private List<StoryMemory> currentMemories(Long projectId, String memoryType, int limit) {
        return storyMemoryMapper.selectList(new LambdaQueryWrapper<StoryMemory>()
                .eq(StoryMemory::getProjectId, projectId)
                .eq(StoryMemory::getMemoryType, memoryType)
                .eq(StoryMemory::getCurrent, true)
                .orderByAsc(StoryMemory::getSequenceNo)
                .last("LIMIT " + limit));
    }

    private StoryMemory currentGlobalMemory(Long projectId) {
        return storyMemoryMapper.selectOne(new LambdaQueryWrapper<StoryMemory>()
                .eq(StoryMemory::getProjectId, projectId)
                .eq(StoryMemory::getMemoryType, "global")
                .eq(StoryMemory::getCurrent, true)
                .last("LIMIT 1"));
    }

    private List<ChapterSummary> recentSummaries(Long projectId) {
        Integer compressedUntil = latestCompressedChapterNo(projectId);
        return chapterSummaryMapper.selectList(new LambdaQueryWrapper<ChapterSummary>()
                .eq(ChapterSummary::getProjectId, projectId)
                .gt(compressedUntil != null, ChapterSummary::getChapterNo, compressedUntil)
                .orderByAsc(ChapterSummary::getChapterNo)
                .last("LIMIT " + RECENT_WINDOW_SIZE));
    }

    private Integer latestCompressedChapterNo(Long projectId) {
        StoryMemory latest = storyMemoryMapper.selectOne(new LambdaQueryWrapper<StoryMemory>()
                .eq(StoryMemory::getProjectId, projectId)
                .in(StoryMemory::getMemoryType, List.of("middle", "high"))
                .isNotNull(StoryMemory::getEndChapterNo)
                .orderByDesc(StoryMemory::getEndChapterNo)
                .last("LIMIT 1"));
        return latest == null ? null : latest.getEndChapterNo();
    }

    private String summaryText(ChapterSummary summary) {
        return "第 %d 章：%s".formatted(summary.getChapterNo(), blankToEmpty(summary.getSummary()));
    }

    private String memoryText(StoryMemory memory) {
        return "%s（第 %s-%s 章）：%s".formatted(
                blankToEmpty(memory.getMemoryKey()),
                memory.getStartChapterNo() == null ? "?" : memory.getStartChapterNo(),
                memory.getEndChapterNo() == null ? "?" : memory.getEndChapterNo(),
                blankToEmpty(memory.getContent()));
    }

    private String blankToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String joinGenres(List<String> genres) {
        return genres == null || genres.isEmpty() ? "" : genres.stream().collect(Collectors.joining(" + "));
    }
}
