package com.jjxmas.ainovelstudio.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jjxmas.ainovelstudio.ai.AiGenerateResult;
import com.jjxmas.ainovelstudio.ai.AiOrchestratorService;
import com.jjxmas.ainovelstudio.common.exception.BusinessException;
import com.jjxmas.ainovelstudio.common.exception.ErrorCode;
import com.jjxmas.ainovelstudio.common.util.JsonUtils;
import com.jjxmas.ainovelstudio.converter.ChapterConverter;
import com.jjxmas.ainovelstudio.mapper.ChapterMapper;
import com.jjxmas.ainovelstudio.mapper.ChapterSummaryMapper;
import com.jjxmas.ainovelstudio.mapper.ForeshadowThreadMapper;
import com.jjxmas.ainovelstudio.mapper.OutlineMapper;
import com.jjxmas.ainovelstudio.mapper.ProjectMapper;
import com.jjxmas.ainovelstudio.mapper.SettingLibraryMapper;
import com.jjxmas.ainovelstudio.mapper.StoryArcMapper;
import com.jjxmas.ainovelstudio.mapper.VolumeMapper;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterOutlineContinueRequest;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterResponse;
import com.jjxmas.ainovelstudio.pojo.entity.Chapter;
import com.jjxmas.ainovelstudio.pojo.entity.ChapterSummary;
import com.jjxmas.ainovelstudio.pojo.entity.ForeshadowThread;
import com.jjxmas.ainovelstudio.pojo.entity.Outline;
import com.jjxmas.ainovelstudio.pojo.entity.Project;
import com.jjxmas.ainovelstudio.pojo.entity.SettingLibrary;
import com.jjxmas.ainovelstudio.pojo.entity.StoryArc;
import com.jjxmas.ainovelstudio.pojo.entity.Volume;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class ChapterOutlineContinuationService {

    private static final Set<Integer> ALLOWED_COUNTS = Set.of(10, 20, 50);
    private static final int MODEL_BATCH_SIZE = 10;

    private final ProjectMapper projectMapper;
    private final SettingLibraryMapper settingLibraryMapper;
    private final OutlineMapper outlineMapper;
    private final VolumeMapper volumeMapper;
    private final StoryArcMapper storyArcMapper;
    private final ChapterMapper chapterMapper;
    private final ChapterSummaryMapper chapterSummaryMapper;
    private final ForeshadowThreadMapper foreshadowThreadMapper;
    private final AiOrchestratorService aiOrchestratorService;
    private final GenerationJobService generationJobService;
    private final VersionService versionService;
    private final ChapterConverter chapterConverter;
    private final TransactionTemplate transactionTemplate;

    @CacheEvict(value = "chapterContextOutlines", key = "#projectId")
    public List<ChapterResponse> continueChapterOutlines(
            Long projectId,
            ChapterOutlineContinueRequest request) {
        validateCount(request.getCount());
        Project project = requireProject(projectId);
        Outline outline = requireConfirmedOutline(projectId);

        Chapter lastChapter = chapterMapper.selectOne(new LambdaQueryWrapper<Chapter>()
                .eq(Chapter::getProjectId, projectId)
                .orderByDesc(Chapter::getChapterNo)
                .last("LIMIT 1"));
        int originalLastChapterNo = lastChapter == null ? 0 : lastChapter.getChapterNo();
        int nextChapterNo = lastChapter == null ? 1 : lastChapter.getChapterNo() + 1;
        int remaining = request.getCount();
        Map<String, Object> baseContext = buildBaseContext(project, outline);
        List<VolumePlan> pendingVolumes = new ArrayList<>();
        List<ArcPlan> pendingArcs = new ArrayList<>();
        List<ChapterPlan> pendingChapters = new ArrayList<>(remaining);
        List<GeneratedBatch> generatedBatches = new ArrayList<>();

        while (remaining > 0) {
            int batchCount = Math.min(remaining, MODEL_BATCH_SIZE);
            Map<String, Object> context = buildContext(
                    baseContext,
                    nextChapterNo,
                    batchCount,
                    request.getInstruction(),
                    pendingVolumes,
                    pendingArcs,
                    pendingChapters);
            AiGenerateResult result = aiOrchestratorService.continueChapterOutline(
                    request.getModelConfigId(),
                    context);
            ParsedBatch batch = parseAndValidateBatch(result, nextChapterNo, batchCount);
            pendingVolumes.addAll(batch.newVolumes());
            pendingArcs.addAll(batch.newArcs());
            pendingChapters.addAll(batch.chapters());
            validateReferences(projectId, pendingVolumes, pendingArcs, pendingChapters);
            generatedBatches.add(new GeneratedBatch(context, batch));
            nextChapterNo += batchCount;
            remaining -= batchCount;
        }
        return transactionTemplate.execute(status -> persistGeneratedBatches(
                projectId,
                outline,
                baseContext,
                originalLastChapterNo,
                request.getModelConfigId(),
                generatedBatches,
                pendingVolumes,
                pendingArcs,
                pendingChapters));
    }

    private void validateCount(Integer count) {
        if (count == null || !ALLOWED_COUNTS.contains(count)) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "count 只支持 10、20 或 50");
        }
    }

    private Project requireProjectForUpdate(Long projectId) {
        Project project = projectMapper.selectOne(new LambdaQueryWrapper<Project>()
                .eq(Project::getId, projectId)
                .last("FOR UPDATE"));
        if (project == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "作品不存在");
        }
        return project;
    }

    private Project requireProject(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "作品不存在");
        }
        return project;
    }

    private Outline requireConfirmedOutline(Long projectId) {
        Outline outline = outlineMapper.selectOne(new LambdaQueryWrapper<Outline>()
                .eq(Outline::getProjectId, projectId)
                .last("LIMIT 1"));
        if (outline == null || outline.getConfirmedAt() == null) {
            throw new BusinessException(ErrorCode.WORKFLOW_GATE_NOT_MET, "请先确认全局大纲，再继续生成章节大纲");
        }
        return outline;
    }

    private Map<String, Object> buildBaseContext(Project project, Outline outline) {
        Long projectId = project.getId();
        SettingLibrary setting = settingLibraryMapper.selectOne(new LambdaQueryWrapper<SettingLibrary>()
                .eq(SettingLibrary::getProjectId, projectId)
                .isNotNull(SettingLibrary::getConfirmedAt)
                .last("LIMIT 1"));
        List<Volume> volumes = volumeMapper.selectList(new LambdaQueryWrapper<Volume>()
                .eq(Volume::getProjectId, projectId)
                .orderByAsc(Volume::getVolumeNo));
        List<StoryArc> arcs = storyArcMapper.selectList(new LambdaQueryWrapper<StoryArc>()
                .eq(StoryArc::getProjectId, projectId)
                .orderByAsc(StoryArc::getVolumeId)
                .orderByAsc(StoryArc::getArcNo));
        Map<Long, Integer> volumeNumbers = new HashMap<>();
        for (Volume volume : volumes) {
            volumeNumbers.put(volume.getId(), volume.getVolumeNo());
        }

        Map<String, Object> baseContext = new LinkedHashMap<>();
        baseContext.put("project", projectContext(project));
        baseContext.put("confirmedSetting", settingContext(setting));
        baseContext.put("globalOutline", text(outline.getContent()));
        baseContext.put("existingVolumes", volumes.stream().map(this::volumeContext).toList());
        baseContext.put("existingArcs", arcs.stream().map(arc -> arcContext(arc, volumeNumbers)).toList());
        baseContext.put("recentChapterOutlines", recentChapterContexts(projectId));
        baseContext.put("recentChapterSummaries", recentSummaryContexts(projectId));
        baseContext.put("activeForeshadowThreads", activeThreadContexts(projectId));
        return baseContext;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildContext(
            Map<String, Object> baseContext,
            int startChapterNo,
            int count,
            String instruction,
            List<VolumePlan> pendingVolumes,
            List<ArcPlan> pendingArcs,
            List<ChapterPlan> pendingChapters) {
        Map<String, Object> context = new LinkedHashMap<>(baseContext);
        List<Map<String, Object>> volumes = (List<Map<String, Object>>) baseContext.get("existingVolumes");
        List<Map<String, Object>> arcs = (List<Map<String, Object>>) baseContext.get("existingArcs");
        List<Map<String, Object>> recent = (List<Map<String, Object>>) baseContext.get("recentChapterOutlines");
        List<Map<String, Object>> volumeContexts = new ArrayList<>(volumes);
        volumeContexts.addAll(pendingVolumes.stream().map(this::volumePlanContext).toList());
        List<Map<String, Object>> arcContexts = new ArrayList<>(arcs);
        arcContexts.addAll(pendingArcs.stream().map(this::arcPlanContext).toList());
        List<Map<String, Object>> recentChapterOutlines = new ArrayList<>(recent);
        recentChapterOutlines.addAll(pendingChapters.stream().map(this::chapterPlanContext).toList());
        if (recentChapterOutlines.size() > 20) {
            recentChapterOutlines = new ArrayList<>(recentChapterOutlines.subList(
                    recentChapterOutlines.size() - 20, recentChapterOutlines.size()));
        }
        context.put("existingVolumes", volumeContexts);
        context.put("existingArcs", arcContexts);
        context.put("recentChapterOutlines", recentChapterOutlines);
        context.put("startChapterNo", startChapterNo);
        context.put("count", count);
        context.put("instruction", text(instruction));
        return context;
    }

    private Map<String, Object> volumePlanContext(VolumePlan volume) {
        return Map.of(
                "volumeNo", volume.volumeNo(),
                "title", volume.title(),
                "summary", volume.summary(),
                "goal", volume.goal(),
                "estimatedWordCount", volume.estimatedWordCount());
    }

    private Map<String, Object> arcPlanContext(ArcPlan arc) {
        return Map.of(
                "volumeNo", arc.volumeNo(),
                "arcNo", arc.arcNo(),
                "title", arc.title(),
                "summary", arc.summary(),
                "goal", arc.goal(),
                "conflict", arc.conflict(),
                "estimatedChapterCount", arc.estimatedChapterCount());
    }

    private Map<String, Object> chapterPlanContext(ChapterPlan chapter) {
        return Map.of(
                "chapterNo", chapter.chapterNo(),
                "title", chapter.title(),
                "outline", chapter.outline(),
                "scenePlan", chapter.scenePlan());
    }

    private Map<String, Object> projectContext(Project project) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("title", text(project.getTitle()));
        value.put("genres", project.getGenres() == null ? List.of() : project.getGenres());
        value.put("projectBrief", text(project.getProjectBrief()));
        value.put("targetWordCountMin", number(project.getTargetWordCountMin()));
        value.put("targetWordCountMax", number(project.getTargetWordCountMax()));
        value.put("targetChapterWordCount", number(project.getTargetChapterWordCount()));
        value.put("platformTarget", text(project.getPlatformTarget()));
        value.put("stylePreference", text(project.getStylePreference()));
        return value;
    }

    private Map<String, Object> settingContext(SettingLibrary setting) {
        if (setting == null) {
            return Map.of("summary", "", "overview", "");
        }
        return Map.of(
                "summary", text(setting.getSummary()),
                "overview", text(setting.getOverview()));
    }

    private Map<String, Object> volumeContext(Volume volume) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("volumeNo", volume.getVolumeNo());
        value.put("title", text(volume.getTitle()));
        value.put("summary", text(volume.getSummary()));
        value.put("goal", text(volume.getGoal()));
        value.put("estimatedWordCount", number(volume.getEstimatedWordCount()));
        return value;
    }

    private Map<String, Object> arcContext(StoryArc arc, Map<Long, Integer> volumeNumbers) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("volumeNo", volumeNumbers.get(arc.getVolumeId()));
        value.put("arcNo", arc.getArcNo());
        value.put("title", text(arc.getTitle()));
        value.put("summary", text(arc.getSummary()));
        value.put("goal", text(arc.getGoal()));
        value.put("conflict", text(arc.getConflict()));
        value.put("estimatedChapterCount", number(arc.getEstimatedChapterCount()));
        return value;
    }

    private List<Map<String, Object>> recentChapterContexts(Long projectId) {
        List<Chapter> chapters = chapterMapper.selectList(new LambdaQueryWrapper<Chapter>()
                .eq(Chapter::getProjectId, projectId)
                .orderByDesc(Chapter::getChapterNo)
                .last("LIMIT 20"));
        Collections.reverse(chapters);
        return chapters.stream().map(chapter -> Map.<String, Object>of(
                "chapterNo", chapter.getChapterNo(),
                "title", text(chapter.getTitle()),
                "outline", text(chapter.getOutline()),
                "scenePlan", JsonUtils.toStringList(chapter.getScenePlan()))).toList();
    }

    private List<Map<String, Object>> recentSummaryContexts(Long projectId) {
        List<ChapterSummary> summaries = chapterSummaryMapper.selectList(new LambdaQueryWrapper<ChapterSummary>()
                .eq(ChapterSummary::getProjectId, projectId)
                .orderByDesc(ChapterSummary::getChapterNo)
                .last("LIMIT 10"));
        Collections.reverse(summaries);
        return summaries.stream().map(summary -> Map.<String, Object>of(
                "chapterNo", summary.getChapterNo(),
                "summary", text(summary.getSummary()),
                "keyEvents", text(summary.getKeyEvents()),
                "foreshadowChanges", text(summary.getForeshadowChanges()))).toList();
    }

    private List<Map<String, Object>> activeThreadContexts(Long projectId) {
        return foreshadowThreadMapper.selectList(new LambdaQueryWrapper<ForeshadowThread>()
                        .eq(ForeshadowThread::getProjectId, projectId)
                        .eq(ForeshadowThread::getStatus, "active")
                        .orderByDesc(ForeshadowThread::getPriority)
                        .last("LIMIT 20"))
                .stream()
                .map(thread -> Map.<String, Object>of(
                        "threadKey", text(thread.getThreadKey()),
                        "title", text(thread.getThreadTitle()),
                        "setup", text(thread.getSetupText()),
                        "latestProgress", text(thread.getLatestProgress()),
                        "payoffHint", text(thread.getPayoffHint())))
                .toList();
    }

    private ParsedBatch parseAndValidateBatch(
            AiGenerateResult result,
            int expectedStart,
            int expectedCount) {
        if (result == null || Boolean.FALSE.equals(result.getSuccess()) || text(result.getContent()).isBlank()) {
            throw new BusinessException(ErrorCode.AI_TASK_UNAVAILABLE, "章节大纲续写模型未返回有效结果");
        }
        Map<String, Object> raw = JsonUtils.toMap(result.getContent());
        if (raw.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "章节大纲续写结果不是合法 JSON");
        }
        List<VolumePlan> newVolumes = mapList(raw.get("newVolumes"), "newVolumes").stream()
                .map(this::parseVolume)
                .toList();
        List<ArcPlan> newArcs = mapList(raw.get("newArcs"), "newArcs").stream()
                .map(this::parseArc)
                .toList();
        List<ChapterPlan> chapters = mapList(raw.get("chapters"), "chapters").stream()
                .map(this::parseChapter)
                .toList();
        if (chapters.size() != expectedCount) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "模型返回的章节数量与请求不一致");
        }
        for (int index = 0; index < chapters.size(); index++) {
            if (chapters.get(index).chapterNo() != expectedStart + index) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "模型返回的章节编号不连续");
            }
        }
        return new ParsedBatch(raw, newVolumes, newArcs, chapters);
    }

    private List<ChapterResponse> persistGeneratedBatches(
            Long projectId,
            Outline originalOutline,
            Map<String, Object> originalBaseContext,
            int originalLastChapterNo,
            Long modelConfigId,
            List<GeneratedBatch> generatedBatches,
            List<VolumePlan> pendingVolumes,
            List<ArcPlan> pendingArcs,
            List<ChapterPlan> pendingChapters) {
        Project currentProject = requireProjectForUpdate(projectId);
        Outline currentOutline = requireConfirmedOutline(projectId);
        if (!Objects.equals(originalOutline.getId(), currentOutline.getId())
                || !Objects.equals(originalOutline.getContent(), currentOutline.getContent())
                || !Objects.equals(originalOutline.getUpdatedAt(), currentOutline.getUpdatedAt())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "全局大纲已变化，请重新生成章节大纲");
        }
        if (!Objects.equals(originalBaseContext, buildBaseContext(currentProject, currentOutline))) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "续写上下文已变化，请重新生成章节大纲");
        }
        Chapter currentLastChapter = chapterMapper.selectOne(new LambdaQueryWrapper<Chapter>()
                .eq(Chapter::getProjectId, projectId)
                .orderByDesc(Chapter::getChapterNo)
                .last("LIMIT 1"));
        int currentLastChapterNo = currentLastChapter == null ? 0 : currentLastChapter.getChapterNo();
        if (currentLastChapterNo != originalLastChapterNo) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "章节大纲已变化，请基于最新章节继续生成");
        }
        validateReferences(projectId, pendingVolumes, pendingArcs, pendingChapters);

        List<ChapterResponse> created = new ArrayList<>(pendingChapters.size());
        for (GeneratedBatch generatedBatch : generatedBatches) {
            Long jobId = generationJobService.recordFinishedJob(
                    projectId,
                    "chapter_outline_continuation",
                    "global_outline",
                    currentOutline.getId(),
                    modelConfigId,
                    generatedBatch.context(),
                    generatedBatch.batch().raw());
            created.addAll(persistBatch(projectId, generatedBatch.batch(), modelConfigId, jobId));
        }
        return created;
    }

    private void validateReferences(
            Long projectId,
            List<VolumePlan> newVolumes,
            List<ArcPlan> newArcs,
            List<ChapterPlan> chapters) {
        List<Volume> existingVolumes = volumeMapper.selectList(new LambdaQueryWrapper<Volume>()
                .eq(Volume::getProjectId, projectId));
        Set<Integer> volumeNumbers = new HashSet<>();
        Map<Long, Integer> volumeNumberById = new HashMap<>();
        for (Volume volume : existingVolumes) {
            volumeNumbers.add(volume.getVolumeNo());
            volumeNumberById.put(volume.getId(), volume.getVolumeNo());
        }
        for (VolumePlan volume : newVolumes) {
            if (!volumeNumbers.add(volume.volumeNo())) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "模型返回了重复的分卷编号");
            }
        }

        Set<ArcKey> arcKeys = new HashSet<>();
        for (StoryArc arc : storyArcMapper.selectList(new LambdaQueryWrapper<StoryArc>()
                .eq(StoryArc::getProjectId, projectId))) {
            Integer volumeNo = volumeNumberById.get(arc.getVolumeId());
            if (volumeNo != null) {
                arcKeys.add(new ArcKey(volumeNo, arc.getArcNo()));
            }
        }
        for (ArcPlan arc : newArcs) {
            if (!volumeNumbers.contains(arc.volumeNo())) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "新剧情单元引用了不存在的分卷");
            }
            if (!arcKeys.add(new ArcKey(arc.volumeNo(), arc.arcNo()))) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "模型返回了重复的剧情单元编号");
            }
        }
        for (ChapterPlan chapter : chapters) {
            if (!arcKeys.contains(new ArcKey(chapter.volumeNo(), chapter.arcNo()))) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "章节引用了不存在的分卷或剧情单元");
            }
        }
    }

    private List<ChapterResponse> persistBatch(
            Long projectId,
            ParsedBatch batch,
            Long modelConfigId,
            Long jobId) {
        Map<Integer, Volume> volumes = new HashMap<>();
        for (Volume volume : volumeMapper.selectList(new LambdaQueryWrapper<Volume>()
                .eq(Volume::getProjectId, projectId))) {
            volumes.put(volume.getVolumeNo(), volume);
        }
        for (VolumePlan plan : batch.newVolumes()) {
            Volume volume = new Volume()
                    .setProjectId(projectId)
                    .setVolumeNo(plan.volumeNo())
                    .setTitle(plan.title())
                    .setSummary(plan.summary())
                    .setGoal(plan.goal())
                    .setEstimatedWordCount(plan.estimatedWordCount());
            volumeMapper.insert(volume);
            volumes.put(volume.getVolumeNo(), volume);
        }

        Map<ArcKey, StoryArc> arcs = new HashMap<>();
        Map<Long, Integer> volumeNumbers = new HashMap<>();
        volumes.values().forEach(volume -> volumeNumbers.put(volume.getId(), volume.getVolumeNo()));
        for (StoryArc arc : storyArcMapper.selectList(new LambdaQueryWrapper<StoryArc>()
                .eq(StoryArc::getProjectId, projectId))) {
            arcs.put(new ArcKey(volumeNumbers.get(arc.getVolumeId()), arc.getArcNo()), arc);
        }
        for (ArcPlan plan : batch.newArcs()) {
            Volume volume = volumes.get(plan.volumeNo());
            StoryArc arc = new StoryArc()
                    .setProjectId(projectId)
                    .setVolumeId(volume.getId())
                    .setArcNo(plan.arcNo())
                    .setTitle(plan.title())
                    .setSummary(plan.summary())
                    .setGoal(plan.goal())
                    .setConflict(plan.conflict())
                    .setEstimatedChapterCount(plan.estimatedChapterCount());
            storyArcMapper.insert(arc);
            arcs.put(new ArcKey(plan.volumeNo(), plan.arcNo()), arc);
        }

        List<ChapterResponse> created = new ArrayList<>(batch.chapters().size());
        for (ChapterPlan plan : batch.chapters()) {
            Volume volume = volumes.get(plan.volumeNo());
            StoryArc arc = arcs.get(new ArcKey(plan.volumeNo(), plan.arcNo()));
            Chapter chapter = new Chapter()
                    .setProjectId(projectId)
                    .setVolumeId(volume.getId())
                    .setStoryArcId(arc.getId())
                    .setChapterNo(plan.chapterNo())
                    .setTitle(plan.title())
                    .setOutline(plan.outline())
                    .setScenePlan(JsonUtils.toJson(plan.scenePlan()))
                    .setStatus("outline_ready");
            chapterMapper.insert(chapter);
            versionService.recordVersion(
                    projectId,
                    "chapter_outline",
                    chapter.getId(),
                    chapterSnapshot(chapter, plan.scenePlan()),
                    "ai_generate",
                    "继续生成章节大纲",
                    modelConfigId,
                    jobId);
            created.add(chapterConverter.toResponse(chapter));
        }
        return created;
    }

    private Map<String, Object> chapterSnapshot(Chapter chapter, List<String> scenePlan) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("chapterNo", chapter.getChapterNo());
        snapshot.put("title", chapter.getTitle());
        snapshot.put("outline", chapter.getOutline());
        snapshot.put("scenePlan", scenePlan);
        return snapshot;
    }

    private VolumePlan parseVolume(Map<String, Object> value) {
        return new VolumePlan(
                positiveInt(value, "volumeNo"),
                requiredText(value, "title"),
                requiredText(value, "summary"),
                requiredText(value, "goal"),
                positiveInt(value, "estimatedWordCount"));
    }

    private ArcPlan parseArc(Map<String, Object> value) {
        return new ArcPlan(
                positiveInt(value, "volumeNo"),
                positiveInt(value, "arcNo"),
                requiredText(value, "title"),
                requiredText(value, "summary"),
                requiredText(value, "goal"),
                requiredText(value, "conflict"),
                positiveInt(value, "estimatedChapterCount"));
    }

    private ChapterPlan parseChapter(Map<String, Object> value) {
        Object scenePlanValue = value.get("scenePlan");
        if (!(scenePlanValue instanceof List<?> values)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "章节 scenePlan 必须是数组");
        }
        List<String> scenePlan = values.stream()
                .map(item -> item instanceof String string ? string.trim() : "")
                .filter(item -> !item.isBlank())
                .toList();
        if (scenePlan.size() != values.size()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "章节 scenePlan 只能包含非空字符串");
        }
        return new ChapterPlan(
                positiveInt(value, "chapterNo"),
                positiveInt(value, "volumeNo"),
                positiveInt(value, "arcNo"),
                requiredText(value, "title"),
                requiredText(value, "outline"),
                scenePlan);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> mapList(Object value, String fieldName) {
        if (value == null && !"chapters".equals(fieldName)) {
            return List.of();
        }
        if (!(value instanceof List<?> values)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, fieldName + " 必须是数组");
        }
        for (Object item : values) {
            if (!(item instanceof Map<?, ?>)) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, fieldName + " 必须包含 JSON 对象");
            }
        }
        return (List<Map<String, Object>>) (List<?>) values;
    }

    private int positiveInt(Map<String, Object> value, String fieldName) {
        Object field = value.get(fieldName);
        if (!(field instanceof Number number) || number.intValue() <= 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, fieldName + " 必须是正整数");
        }
        return number.intValue();
    }

    private String requiredText(Map<String, Object> value, String fieldName) {
        String field = text(value.get(fieldName));
        if (field.isBlank()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, fieldName + " 不能为空");
        }
        return field;
    }

    private int number(Integer value) {
        return value == null ? 0 : value;
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private record VolumePlan(
            int volumeNo,
            String title,
            String summary,
            String goal,
            int estimatedWordCount) {
    }

    private record ArcPlan(
            int volumeNo,
            int arcNo,
            String title,
            String summary,
            String goal,
            String conflict,
            int estimatedChapterCount) {
    }

    private record ChapterPlan(
            int chapterNo,
            int volumeNo,
            int arcNo,
            String title,
            String outline,
            List<String> scenePlan) {
    }

    private record ArcKey(int volumeNo, int arcNo) {
    }

    private record ParsedBatch(
            Map<String, Object> raw,
            List<VolumePlan> newVolumes,
            List<ArcPlan> newArcs,
            List<ChapterPlan> chapters) {
    }

    private record GeneratedBatch(Map<String, Object> context, ParsedBatch batch) {
    }
}
