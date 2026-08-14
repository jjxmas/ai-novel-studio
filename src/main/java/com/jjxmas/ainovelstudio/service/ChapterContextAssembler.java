package com.jjxmas.ainovelstudio.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jjxmas.ainovelstudio.common.util.JsonUtils;
import com.jjxmas.ainovelstudio.mapper.ChapterMapper;
import com.jjxmas.ainovelstudio.mapper.ChapterSummaryMapper;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterContext;
import com.jjxmas.ainovelstudio.pojo.entity.Chapter;
import com.jjxmas.ainovelstudio.pojo.entity.ChapterSummary;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ChapterContextAssembler {

    private static final int PREVIOUS_CHAPTER_TAIL_LENGTH = 1600;

    private final ChapterSummaryMapper chapterSummaryMapper;
    private final ChapterMapper chapterMapper;
    private final ChapterContextCacheSource contextCacheSource;
    private final StoryStateSnapshotService storyStateSnapshotService;
    private final ForeshadowThreadService foreshadowThreadService;
    private final StoryDirtyMarkService storyDirtyMarkService;

    public ChapterContextAssembler(
            ChapterSummaryMapper chapterSummaryMapper,
            ChapterMapper chapterMapper,
            ChapterContextCacheSource contextCacheSource,
            StoryStateSnapshotService storyStateSnapshotService,
            ForeshadowThreadService foreshadowThreadService,
            StoryDirtyMarkService storyDirtyMarkService) {
        this.chapterSummaryMapper = chapterSummaryMapper;
        this.chapterMapper = chapterMapper;
        this.contextCacheSource = contextCacheSource;
        this.storyStateSnapshotService = storyStateSnapshotService;
        this.foreshadowThreadService = foreshadowThreadService;
        this.storyDirtyMarkService = storyDirtyMarkService;
    }

    public ChapterContext assemble(Chapter chapter, String titleOverride, String outlineOverride, String userAdvice) {
        Long projectId = chapter.getProjectId();
        ChapterContext.ProjectProfile projectProfile = contextCacheSource.projectProfile(projectId);
        ChapterContext.ImmutableSetting immutableSetting = contextCacheSource.immutableSetting(projectId);
        ChapterContext.StoryPlan storyPlan = contextCacheSource.storyPlan(projectId);
        ChapterContext.MemoryStack memoryStack = contextCacheSource.memoryStack(projectId);

        Chapter previousChapter = previousChapter(chapter);
        ChapterSummary previousSummary = previousChapter == null ? null : chapterSummaryMapper.selectOne(new LambdaQueryWrapper<ChapterSummary>()
                .eq(ChapterSummary::getChapterId, previousChapter.getId())
                .last("LIMIT 1"));

        ChapterContext.CurrentChapter currentChapter = currentChapter(chapter, titleOverride, outlineOverride);
        String previousSummaryText = previousSummary == null ? "" : blankToEmpty(previousSummary.getSummary());
        ChapterContext.CurrentState currentState = storyStateSnapshotService.snapshotForChapter(
                chapter,
                currentChapter.getTitle(),
                currentChapter.getOutline(),
                currentChapter.getScenePlan(),
                previousSummaryText);
        ChapterContext.ActiveThreads activeThreads = foreshadowThreadService.buildActiveThreads(
                chapter,
                currentChapter.getTitle(),
                currentChapter.getOutline(),
                currentChapter.getScenePlan(),
                previousSummaryText);

        return ChapterContext.builder()
                .projectProfile(projectProfile)
                .immutableSetting(immutableSetting)
                .storyPlan(storyPlan)
                .currentChapter(currentChapter)
                .continuity(continuity(previousChapter, previousSummary))
                .currentState(currentState)
                .activeThreads(activeThreads)
                .memoryStack(memoryStack)
                .generationConstraints(generationConstraints(projectProfile, chapter, userAdvice))
                .build();
    }

    public Map<String, Object> asLogMap(ChapterContext context) {
        Map<String, Object> log = new LinkedHashMap<>();
        log.put("projectProfile", context.getProjectProfile());
        log.put("immutableSetting", context.getImmutableSetting());
        log.put("storyPlan", context.getStoryPlan());
        log.put("currentChapter", context.getCurrentChapter());
        log.put("continuity", context.getContinuity());
        log.put("currentState", context.getCurrentState());
        log.put("activeThreads", context.getActiveThreads());
        log.put("memoryStack", context.getMemoryStack());
        log.put("generationConstraints", context.getGenerationConstraints());
        return log;
    }

    private ChapterContext.CurrentChapter currentChapter(Chapter chapter, String titleOverride, String outlineOverride) {
        String title = titleOverride == null || titleOverride.isBlank() ? chapter.getTitle() : titleOverride;
        String outline = outlineOverride == null || outlineOverride.isBlank() ? chapter.getOutline() : outlineOverride;
        return ChapterContext.CurrentChapter.builder()
                .chapterId(chapter.getId())
                .chapterNo(chapter.getChapterNo() == null ? 0 : chapter.getChapterNo())
                .title(blankToEmpty(title))
                .outline(blankToEmpty(outline))
                .scenePlan(JsonUtils.toStringList(chapter.getScenePlan()))
                .build();
    }

    private ChapterContext.Continuity continuity(Chapter previousChapter, ChapterSummary previousSummary) {
        if (previousChapter == null) {
            return ChapterContext.Continuity.builder()
                    .hasPreviousChapter(false)
                    .previousChapterNo(0)
                    .previousChapterTitle("")
                    .previousChapterSummary("这是第一章，无需承接上一章结尾。")
                    .previousKeyEvents(List.of())
                    .previousCharacterChanges(List.of())
                    .previousLocationChanges(List.of())
                    .previousForeshadowChanges(List.of())
                    .previousChapterTail("")
                    .openingRequirement("直接建立当前章节的场景目标，不要假装已经存在上一章结尾。")
                    .carryForwardRequirements(List.of("当前章节目标", "当前章节限制条件"))
                    .chapterTask("完成当前章节大纲中的目标、阻碍和推进结果。")
                    .build();
        }
        return ChapterContext.Continuity.builder()
                .hasPreviousChapter(true)
                .previousChapterNo(previousChapter.getChapterNo() == null ? 0 : previousChapter.getChapterNo())
                .previousChapterTitle(blankToEmpty(previousChapter.getTitle()))
                .previousChapterSummary(previousSummary == null ? "" : blankToEmpty(previousSummary.getSummary()))
                .previousKeyEvents(previousSummary == null ? List.of() : JsonUtils.toStringList(previousSummary.getKeyEvents()))
                .previousCharacterChanges(previousSummary == null ? List.of() : JsonUtils.toStringList(previousSummary.getCharacterChanges()))
                .previousLocationChanges(previousSummary == null ? List.of() : JsonUtils.toStringList(previousSummary.getLocationChanges()))
                .previousForeshadowChanges(previousSummary == null ? List.of() : JsonUtils.toStringList(previousSummary.getForeshadowChanges()))
                .previousChapterTail(tailText(previousChapter.getContent(), PREVIOUS_CHAPTER_TAIL_LENGTH))
                .openingRequirement("从上一章最后的动作、对话、地点或状态自然接续开场，不要重新起头。")
                .carryForwardRequirements(List.of("人物当前状态", "时间线", "地点", "未解决目标", "设定代价"))
                .chapterTask("完成当前章节大纲中的目标、阻碍和推进结果，并保留自然钩子。")
                .build();
    }

    private ChapterContext.GenerationConstraints generationConstraints(
            ChapterContext.ProjectProfile projectProfile,
            Chapter chapter,
            String userAdvice) {
        return ChapterContext.GenerationConstraints.builder()
                .targetChapterWordCount(projectProfile == null || projectProfile.getTargetChapterWordCount() == null
                        ? 3000
                        : projectProfile.getTargetChapterWordCount())
                .stylePreference(projectProfile == null ? "" : blankToEmpty(projectProfile.getStylePreference()))
                .userAdvice(blankToEmpty(userAdvice))
                .dataQualityWarnings(storyDirtyMarkService.activeWarningsForChapter(chapter))
                .build();
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

    private String blankToEmpty(String value) {
        return value == null ? "" : value;
    }
}
