package com.jjxmas.ainovelstudio.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jjxmas.ainovelstudio.ai.AiGenerateResult;
import com.jjxmas.ainovelstudio.ai.AiOrchestratorService;
import com.jjxmas.ainovelstudio.common.exception.BusinessException;
import com.jjxmas.ainovelstudio.common.exception.ErrorCode;
import com.jjxmas.ainovelstudio.common.util.JsonUtils;
import com.jjxmas.ainovelstudio.converter.OutlineConverter;
import com.jjxmas.ainovelstudio.converter.OutlineWorkflowConverter;
import com.jjxmas.ainovelstudio.mapper.ChapterMapper;
import com.jjxmas.ainovelstudio.mapper.OutlineMapper;
import com.jjxmas.ainovelstudio.mapper.OutlineWorkflowRunMapper;
import com.jjxmas.ainovelstudio.mapper.ProjectMapper;
import com.jjxmas.ainovelstudio.mapper.SettingLibraryMapper;
import com.jjxmas.ainovelstudio.mapper.StoryArcMapper;
import com.jjxmas.ainovelstudio.mapper.VolumeMapper;
import com.jjxmas.ainovelstudio.pojo.dto.OutlineResponse;
import com.jjxmas.ainovelstudio.pojo.dto.OutlineWorkflowCreateRequest;
import com.jjxmas.ainovelstudio.pojo.dto.OutlineWorkflowResponse;
import com.jjxmas.ainovelstudio.pojo.dto.VolumeOutlineResponse;
import com.jjxmas.ainovelstudio.pojo.entity.Chapter;
import com.jjxmas.ainovelstudio.pojo.entity.Outline;
import com.jjxmas.ainovelstudio.pojo.entity.OutlineWorkflowRun;
import com.jjxmas.ainovelstudio.pojo.entity.Project;
import com.jjxmas.ainovelstudio.pojo.entity.SettingLibrary;
import com.jjxmas.ainovelstudio.pojo.entity.StoryArc;
import com.jjxmas.ainovelstudio.pojo.entity.Volume;
import com.jjxmas.ainovelstudio.service.GenerationJobService;
import com.jjxmas.ainovelstudio.service.OutlineWorkflowService;
import com.jjxmas.ainovelstudio.service.VersionService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class OutlineWorkflowServiceImpl implements OutlineWorkflowService {

    private final OutlineWorkflowRunMapper outlineWorkflowRunMapper;
    private final ProjectMapper projectMapper;
    private final SettingLibraryMapper settingLibraryMapper;
    private final OutlineMapper outlineMapper;
    private final VolumeMapper volumeMapper;
    private final StoryArcMapper storyArcMapper;
    private final ChapterMapper chapterMapper;
    private final GenerationJobService generationJobService;
    private final VersionService versionService;
    private final AiOrchestratorService aiOrchestratorService;
    private final OutlineConverter outlineConverter;
    private final OutlineWorkflowConverter outlineWorkflowConverter;
    private final CacheManager cacheManager;
    private final TransactionTemplate transactionTemplate;

    public OutlineWorkflowServiceImpl(
            OutlineWorkflowRunMapper outlineWorkflowRunMapper,
            ProjectMapper projectMapper,
            SettingLibraryMapper settingLibraryMapper,
            OutlineMapper outlineMapper,
            VolumeMapper volumeMapper,
            StoryArcMapper storyArcMapper,
            ChapterMapper chapterMapper,
            GenerationJobService generationJobService,
            VersionService versionService,
            AiOrchestratorService aiOrchestratorService,
            OutlineConverter outlineConverter,
            OutlineWorkflowConverter outlineWorkflowConverter,
            CacheManager cacheManager,
            TransactionTemplate transactionTemplate) {
        this.outlineWorkflowRunMapper = outlineWorkflowRunMapper;
        this.projectMapper = projectMapper;
        this.settingLibraryMapper = settingLibraryMapper;
        this.outlineMapper = outlineMapper;
        this.volumeMapper = volumeMapper;
        this.storyArcMapper = storyArcMapper;
        this.chapterMapper = chapterMapper;
        this.generationJobService = generationJobService;
        this.versionService = versionService;
        this.aiOrchestratorService = aiOrchestratorService;
        this.outlineConverter = outlineConverter;
        this.outlineWorkflowConverter = outlineWorkflowConverter;
        this.cacheManager = cacheManager;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public OutlineWorkflowResponse startWorkflow(OutlineWorkflowCreateRequest request) {
        Project project = requireProject(request.getProjectId());
        SettingLibrary setting = requireConfirmedSetting(project.getId());
        Map<String, Object> context = outlineContext(project, setting);
        AiGenerateResult result = aiOrchestratorService.generateOutlineWorkflowDraft(request.getModelConfigId(), context);
        Map<String, Object> draft = requireJsonObject(result.getContent(), "大纲草案不是合法 JSON");
        Map<String, Object> checks = checkDraft(draft);
        boolean passed = Boolean.TRUE.equals(checks.get("passed"));
        return transactionTemplate.execute(status -> {
            Project currentProject = requireProject(project.getId());
            SettingLibrary currentSetting = requireConfirmedSetting(currentProject.getId());
            if (!setting.getId().equals(currentSetting.getId())) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "设定库已变化，请重新生成大纲草案");
            }
            OutlineWorkflowRun run = new OutlineWorkflowRun()
                    .setProjectId(currentProject.getId())
                    .setSettingLibraryId(currentSetting.getId())
                    .setModelConfigId(request.getModelConfigId())
                    .setStatus(passed ? "draft_ready" : "check_failed")
                    .setDraftJson(JsonUtils.toJson(draft))
                    .setCheckJson(JsonUtils.toJson(checks));
            outlineWorkflowRunMapper.insert(run);
            generationJobService.recordFinishedJob(currentProject.getId(), "outline_workflow_draft",
                    "outline_workflow", run.getId(), request.getModelConfigId(), context,
                    Map.of("draft", draft, "checks", checks));
            return toResponse(run);
        });
    }

    @Override
    public OutlineWorkflowResponse getWorkflow(Long workflowId) {
        return toResponse(requireRun(workflowId));
    }

    @Override
    public OutlineWorkflowResponse getLatestWorkflow(Long projectId) {
        requireProject(projectId);
        OutlineWorkflowRun run = outlineWorkflowRunMapper.selectOne(new LambdaQueryWrapper<OutlineWorkflowRun>()
                .eq(OutlineWorkflowRun::getProjectId, projectId)
                .orderByDesc(OutlineWorkflowRun::getId)
                .last("LIMIT 1"));
        if (run == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "大纲生成工作流不存在");
        }
        return toResponse(run);
    }

    @Override
    @Transactional
    public OutlineResponse commitWorkflow(Long workflowId) {
        OutlineWorkflowRun run = requireRunForUpdate(workflowId);
        if ("committed".equals(run.getStatus())) {
            Outline outline = outlineMapper.selectOne(new LambdaQueryWrapper<Outline>()
                    .eq(Outline::getProjectId, run.getProjectId())
                    .last("LIMIT 1"));
            if (outline == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "outline not found");
            }
            return toOutlineResponse(outline);
        }
        if (!"draft_ready".equals(run.getStatus())) {
            throw new BusinessException(ErrorCode.WORKFLOW_GATE_NOT_MET, "大纲草案通过检查后才能提交");
        }
        Long projectId = run.getProjectId();
        List<Chapter> existingChapters = chapterMapper.selectList(new LambdaQueryWrapper<Chapter>()
                .eq(Chapter::getProjectId, projectId));
        boolean hasWrittenContent = existingChapters.stream().anyMatch(chapter -> !text(chapter.getContent()).isBlank());
        if (hasWrittenContent) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "已有章节正文，不能覆盖章节大纲");
        }

        Map<String, Object> draft = JsonUtils.toMap(run.getDraftJson());
        Map<String, Object> globalOutline = mapValue(draft.get("globalOutline"));
        Outline outline = outlineMapper.selectOne(new LambdaQueryWrapper<Outline>()
                .eq(Outline::getProjectId, projectId)
                .last("LIMIT 1"));
        if (outline == null) {
            outline = new Outline().setProjectId(projectId);
        }
        outline.setTitle(defaultText(globalOutline, "title", "全局大纲"))
                .setContent(requiredText(globalOutline, "content", "全局大纲内容不能为空"))
                .setConfirmedAt(LocalDateTime.now());
        if (outline.getId() == null) {
            outlineMapper.insert(outline);
        } else {
            outlineMapper.updateById(outline);
        }

        chapterMapper.delete(new LambdaQueryWrapper<Chapter>().eq(Chapter::getProjectId, projectId));
        storyArcMapper.delete(new LambdaQueryWrapper<StoryArc>().eq(StoryArc::getProjectId, projectId));
        volumeMapper.delete(new LambdaQueryWrapper<Volume>().eq(Volume::getProjectId, projectId));

        Map<Integer, Long> volumeIds = new HashMap<>();
        for (Map<String, Object> item : listOfMaps(draft.get("volumes"))) {
            Volume volume = new Volume()
                    .setProjectId(projectId)
                    .setVolumeNo(intValue(item.get("volumeNo")))
                    .setTitle(requiredText(item, "title", "分卷标题不能为空"))
                    .setSummary(text(item.get("summary")))
                    .setGoal(text(item.get("goal")))
                    .setEstimatedWordCount(intValue(item.get("estimatedWordCount")));
            volumeMapper.insert(volume);
            volumeIds.put(volume.getVolumeNo(), volume.getId());
        }

        Map<String, Long> arcIds = new HashMap<>();
        for (Map<String, Object> item : listOfMaps(draft.get("arcs"))) {
            int volumeNo = intValue(item.get("volumeNo"));
            int arcNo = intValue(item.get("arcNo"));
            StoryArc arc = new StoryArc()
                    .setProjectId(projectId)
                    .setVolumeId(volumeIds.get(volumeNo))
                    .setArcNo(arcNo)
                    .setTitle(requiredText(item, "title", "剧情单元标题不能为空"))
                    .setSummary(text(item.get("summary")))
                    .setGoal(text(item.get("goal")))
                    .setConflict(text(item.get("conflict")))
                    .setEstimatedChapterCount(intValue(item.get("estimatedChapterCount")));
            storyArcMapper.insert(arc);
            arcIds.put(arcKey(volumeNo, arcNo), arc.getId());
        }

        for (Map<String, Object> item : listOfMaps(draft.get("chapters"))) {
            int volumeNo = intValue(item.get("volumeNo"));
            int arcNo = intValue(item.get("arcNo"));
            Chapter chapter = new Chapter()
                    .setProjectId(projectId)
                    .setVolumeId(volumeIds.get(volumeNo))
                    .setStoryArcId(arcIds.get(arcKey(volumeNo, arcNo)))
                    .setChapterNo(intValue(item.get("chapterNo")))
                    .setTitle(requiredText(item, "title", "章节标题不能为空"))
                    .setOutline(requiredText(item, "outline", "章节大纲不能为空"))
                    .setScenePlan(JsonUtils.toJson(item.get("scenePlan")))
                    .setStatus("outline_ready");
            chapterMapper.insert(chapter);
            versionService.recordVersion(projectId, "chapter_outline", chapter.getId(),
                    Map.of("chapterNo", chapter.getChapterNo(), "title", chapter.getTitle(), "outline", chapter.getOutline()),
                    "ai_generate", "大纲 workflow 生成章节大纲", run.getModelConfigId(), null);
        }

        versionService.recordVersion(projectId, "global_outline", outline.getId(),
                Map.of("title", outline.getTitle(), "content", outline.getContent(), "confirmed", true),
                "ai_generate", "大纲 workflow 提交全局大纲", run.getModelConfigId(), null);
        Project project = requireProject(projectId);
        project.setWorkflowStage("chapter");
        projectMapper.updateById(project);
        run.setStatus("committed").setCommittedAt(LocalDateTime.now());
        outlineWorkflowRunMapper.updateById(run);
        evictCommittedContextCaches(projectId);
        return toOutlineResponse(outline);
    }

    private void evictCommittedContextCaches(Long projectId) {
        for (String cacheName : List.of("globalOutlines", "chapterContextOutlines", "projects", "chapterContextProfiles")) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.evict(projectId);
            }
        }
    }

    private Map<String, Object> outlineContext(Project project, SettingLibrary setting) {
        return Map.of(
                "projectTitle", text(project.getTitle()),
                "genres", String.join(" + ", project.getGenres() == null ? List.of() : project.getGenres()),
                "targetWordCountMin", project.getTargetWordCountMin() == null ? 0 : project.getTargetWordCountMin(),
                "targetWordCountMax", project.getTargetWordCountMax() == null ? 0 : project.getTargetWordCountMax(),
                "targetChapterWordCount", project.getTargetChapterWordCount() == null ? 0 : project.getTargetChapterWordCount(),
                "platformTarget", text(project.getPlatformTarget()),
                "stylePreference", text(project.getStylePreference()),
                "settingOverview", text(setting.getOverview()),
                "settingSummary", text(setting.getSummary()));
    }

    private Map<String, Object> checkDraft(Map<String, Object> draft) {
        List<String> issues = new ArrayList<>();
        Map<String, Object> globalOutline = mapValue(draft.get("globalOutline"));
        if (text(globalOutline.get("content")).isBlank()) {
            issues.add("缺少全局大纲内容");
        }
        Set<Integer> volumeNos = new HashSet<>();
        for (Map<String, Object> volume : listOfMaps(draft.get("volumes"))) {
            int volumeNo = intValue(volume.get("volumeNo"));
            if (volumeNo <= 0 || !volumeNos.add(volumeNo)) {
                issues.add("分卷编号无效或重复：" + volumeNo);
            }
            if (text(volume.get("title")).isBlank()) {
                issues.add("分卷标题不能为空");
            }
        }
        Set<String> arcKeys = new HashSet<>();
        for (Map<String, Object> arc : listOfMaps(draft.get("arcs"))) {
            int volumeNo = intValue(arc.get("volumeNo"));
            int arcNo = intValue(arc.get("arcNo"));
            if (!volumeNos.contains(volumeNo)) {
                issues.add("剧情单元引用不存在分卷：" + volumeNo);
            }
            arcKeys.add(arcKey(volumeNo, arcNo));
        }
        Set<Integer> chapterNos = new HashSet<>();
        List<Map<String, Object>> chapters = listOfMaps(draft.get("chapters"));
        if (chapters.size() < 5 || chapters.size() > 10) {
            issues.add("第一批章节大纲应为 5-10 章");
        }
        for (Map<String, Object> chapter : chapters) {
            int chapterNo = intValue(chapter.get("chapterNo"));
            int volumeNo = intValue(chapter.get("volumeNo"));
            int arcNo = intValue(chapter.get("arcNo"));
            if (chapterNo <= 0 || !chapterNos.add(chapterNo)) {
                issues.add("章节编号无效或重复：" + chapterNo);
            }
            if (!arcKeys.contains(arcKey(volumeNo, arcNo))) {
                issues.add("章节引用不存在剧情单元：" + volumeNo + "-" + arcNo);
            }
            if (text(chapter.get("title")).isBlank() || text(chapter.get("outline")).isBlank()) {
                issues.add("章节标题和大纲不能为空");
            }
        }
        return Map.of("passed", issues.isEmpty(), "issues", issues);
    }

    private SettingLibrary requireConfirmedSetting(Long projectId) {
        requireProject(projectId);
        SettingLibrary setting = settingLibraryMapper.selectOne(new LambdaQueryWrapper<SettingLibrary>()
                .eq(SettingLibrary::getProjectId, projectId)
                .isNotNull(SettingLibrary::getConfirmedAt)
                .last("LIMIT 1"));
        if (setting == null) {
            throw new BusinessException(ErrorCode.WORKFLOW_GATE_NOT_MET, "请先确认设定库，再生成大纲");
        }
        return setting;
    }

    private Project requireProject(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "作品不存在");
        }
        return project;
    }

    private OutlineWorkflowRun requireRun(Long workflowId) {
        OutlineWorkflowRun run = outlineWorkflowRunMapper.selectById(workflowId);
        if (run == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "大纲生成工作流不存在");
        }
        return run;
    }

    private OutlineWorkflowRun requireRunForUpdate(Long workflowId) {
        OutlineWorkflowRun run = outlineWorkflowRunMapper.selectOne(new LambdaQueryWrapper<OutlineWorkflowRun>()
                .eq(OutlineWorkflowRun::getId, workflowId)
                .last("FOR UPDATE"));
        if (run == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "outline workflow run not found");
        }
        return run;
    }

    private OutlineWorkflowResponse toResponse(OutlineWorkflowRun run) {
        OutlineWorkflowResponse response = outlineWorkflowConverter.toResponse(run);
        response.setDraft(JsonUtils.toMap(run.getDraftJson()));
        response.setChecks(JsonUtils.toMap(run.getCheckJson()));
        return response;
    }

    private OutlineResponse toOutlineResponse(Outline outline) {
        OutlineResponse response = outlineConverter.toResponse(outline);
        response.setVolumes(outlineConverter.toVolumeResponseList(volumeMapper.selectList(new LambdaQueryWrapper<Volume>()
                .eq(Volume::getProjectId, outline.getProjectId())
                .orderByAsc(Volume::getVolumeNo))));
        return response;
    }

    private Map<String, Object> requireJsonObject(String content, String message) {
        Map<String, Object> map = JsonUtils.toMap(extractJson(content));
        if (map.isEmpty()) {
            throw new BusinessException(ErrorCode.AI_TASK_UNAVAILABLE, message);
        }
        return map;
    }

    private String extractJson(String content) {
        if (content == null) {
            return "";
        }
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        return start >= 0 && end > start ? content.substring(start, end + 1) : content;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> listOfMaps(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                result.add((Map<String, Object>) map);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private String requiredText(Map<String, Object> map, String key, String message) {
        String value = text(map.get(key));
        if (value.isBlank()) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, message);
        }
        return value;
    }

    private String defaultText(Map<String, Object> map, String key, String fallback) {
        String value = text(map.get(key));
        return value.isBlank() ? fallback : value;
    }

    private String arcKey(int volumeNo, int arcNo) {
        return volumeNo + ":" + arcNo;
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private int intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(text(value));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }
}
