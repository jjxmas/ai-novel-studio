package com.jjxmas.ainovelstudio.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jjxmas.ainovelstudio.ai.AiGenerateResult;
import com.jjxmas.ainovelstudio.ai.AiOrchestratorService;
import com.jjxmas.ainovelstudio.common.exception.BusinessException;
import com.jjxmas.ainovelstudio.common.exception.ErrorCode;
import com.jjxmas.ainovelstudio.common.util.JsonUtils;
import com.jjxmas.ainovelstudio.converter.SettingWorkflowConverter;
import com.jjxmas.ainovelstudio.mapper.EntityRelationMapper;
import com.jjxmas.ainovelstudio.mapper.EntityStateRecordMapper;
import com.jjxmas.ainovelstudio.mapper.IdeaMapper;
import com.jjxmas.ainovelstudio.mapper.OrganizationMapper;
import com.jjxmas.ainovelstudio.mapper.ProjectMapper;
import com.jjxmas.ainovelstudio.mapper.SettingWorkflowRunMapper;
import com.jjxmas.ainovelstudio.mapper.StoryCharacterMapper;
import com.jjxmas.ainovelstudio.mapper.StoryEventMapper;
import com.jjxmas.ainovelstudio.mapper.StoryItemMapper;
import com.jjxmas.ainovelstudio.mapper.StoryLocationMapper;
import com.jjxmas.ainovelstudio.mapper.WorldRuleMapper;
import com.jjxmas.ainovelstudio.pojo.dto.SettingLibraryGenerateRequest;
import com.jjxmas.ainovelstudio.pojo.dto.SettingLibraryResponse;
import com.jjxmas.ainovelstudio.pojo.dto.SettingLibraryUpdateRequest;
import com.jjxmas.ainovelstudio.pojo.dto.SettingWorkflowCreateRequest;
import com.jjxmas.ainovelstudio.pojo.dto.SettingWorkflowResponse;
import com.jjxmas.ainovelstudio.pojo.entity.EntityRelation;
import com.jjxmas.ainovelstudio.pojo.entity.EntityStateRecord;
import com.jjxmas.ainovelstudio.pojo.entity.Idea;
import com.jjxmas.ainovelstudio.pojo.entity.Organization;
import com.jjxmas.ainovelstudio.pojo.entity.Project;
import com.jjxmas.ainovelstudio.pojo.entity.SettingWorkflowRun;
import com.jjxmas.ainovelstudio.pojo.entity.StoryCharacter;
import com.jjxmas.ainovelstudio.pojo.entity.StoryEvent;
import com.jjxmas.ainovelstudio.pojo.entity.StoryItem;
import com.jjxmas.ainovelstudio.pojo.entity.StoryLocation;
import com.jjxmas.ainovelstudio.pojo.entity.WorldRule;
import com.jjxmas.ainovelstudio.service.GenerationJobService;
import com.jjxmas.ainovelstudio.service.SettingLibraryService;
import com.jjxmas.ainovelstudio.service.SettingWorkflowService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettingWorkflowServiceImpl implements SettingWorkflowService {

    private final SettingWorkflowRunMapper settingWorkflowRunMapper;
    private final ProjectMapper projectMapper;
    private final IdeaMapper ideaMapper;
    private final StoryCharacterMapper storyCharacterMapper;
    private final OrganizationMapper organizationMapper;
    private final StoryLocationMapper storyLocationMapper;
    private final StoryItemMapper storyItemMapper;
    private final WorldRuleMapper worldRuleMapper;
    private final EntityRelationMapper entityRelationMapper;
    private final StoryEventMapper storyEventMapper;
    private final EntityStateRecordMapper entityStateRecordMapper;
    private final SettingLibraryService settingLibraryService;
    private final GenerationJobService generationJobService;
    private final AiOrchestratorService aiOrchestratorService;
    private final SettingWorkflowConverter settingWorkflowConverter;

    public SettingWorkflowServiceImpl(
            SettingWorkflowRunMapper settingWorkflowRunMapper,
            ProjectMapper projectMapper,
            IdeaMapper ideaMapper,
            StoryCharacterMapper storyCharacterMapper,
            OrganizationMapper organizationMapper,
            StoryLocationMapper storyLocationMapper,
            StoryItemMapper storyItemMapper,
            WorldRuleMapper worldRuleMapper,
            EntityRelationMapper entityRelationMapper,
            StoryEventMapper storyEventMapper,
            EntityStateRecordMapper entityStateRecordMapper,
            SettingLibraryService settingLibraryService,
            GenerationJobService generationJobService,
            AiOrchestratorService aiOrchestratorService,
            SettingWorkflowConverter settingWorkflowConverter) {
        this.settingWorkflowRunMapper = settingWorkflowRunMapper;
        this.projectMapper = projectMapper;
        this.ideaMapper = ideaMapper;
        this.storyCharacterMapper = storyCharacterMapper;
        this.organizationMapper = organizationMapper;
        this.storyLocationMapper = storyLocationMapper;
        this.storyItemMapper = storyItemMapper;
        this.worldRuleMapper = worldRuleMapper;
        this.entityRelationMapper = entityRelationMapper;
        this.storyEventMapper = storyEventMapper;
        this.entityStateRecordMapper = entityStateRecordMapper;
        this.settingLibraryService = settingLibraryService;
        this.generationJobService = generationJobService;
        this.aiOrchestratorService = aiOrchestratorService;
        this.settingWorkflowConverter = settingWorkflowConverter;
    }

    @Override
    @Transactional
    public SettingWorkflowResponse startWorkflow(SettingWorkflowCreateRequest request) {
        Project project = requireProject(request.getProjectId());
        Idea idea = requireSelectedIdea(project, request.getIdeaId());
        Map<String, Object> context = settingContext(project, idea);
        AiGenerateResult result = aiOrchestratorService.generateSettingBlueprint(request.getModelConfigId(), context);
        Map<String, Object> blueprint = requireJsonObject(result.getContent(), "设定蓝图不是合法 JSON");
        List<String> issues = validateBlueprint(blueprint);
        if (!issues.isEmpty()) {
            throw new BusinessException(ErrorCode.AI_TASK_UNAVAILABLE, "设定蓝图校验失败：" + String.join("；", issues));
        }

        SettingWorkflowRun run = new SettingWorkflowRun()
                .setProjectId(project.getId())
                .setSourceIdeaId(idea.getId())
                .setModelConfigId(request.getModelConfigId())
                .setStatus("blueprint_ready")
                .setBlueprintJson(JsonUtils.toJson(blueprint));
        settingWorkflowRunMapper.insert(run);
        generationJobService.recordFinishedJob(project.getId(), "setting_workflow_blueprint", "setting_workflow", run.getId(),
                request.getModelConfigId(), context, blueprint);
        return toResponse(run);
    }

    @Override
    public SettingWorkflowResponse getWorkflow(Long workflowId) {
        return toResponse(requireRun(workflowId));
    }

    @Override
    public SettingWorkflowResponse getLatestWorkflow(Long projectId) {
        requireProject(projectId);
        SettingWorkflowRun run = settingWorkflowRunMapper.selectOne(new LambdaQueryWrapper<SettingWorkflowRun>()
                .eq(SettingWorkflowRun::getProjectId, projectId)
                .orderByDesc(SettingWorkflowRun::getId)
                .last("LIMIT 1"));
        if (run == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "设定生成工作流不存在");
        }
        return toResponse(run);
    }

    @Override
    @Transactional
    public SettingWorkflowResponse approveBlueprint(Long workflowId) {
        SettingWorkflowRun run = requireRun(workflowId);
        if (!"blueprint_ready".equals(run.getStatus())) {
            throw new BusinessException(ErrorCode.WORKFLOW_GATE_NOT_MET, "当前工作流不能确认蓝图");
        }
        Project project = requireProject(run.getProjectId());
        Idea idea = requireSelectedIdea(project, run.getSourceIdeaId());
        Map<String, Object> blueprint = JsonUtils.toMap(run.getBlueprintJson());
        AiGenerateResult result = aiOrchestratorService.generateSettingDraft(run.getModelConfigId(), settingContext(project, idea), blueprint);
        Map<String, Object> draft = requireJsonObject(result.getContent(), "设定草案不是合法 JSON");
        Map<String, Object> checks = checkDraft(draft);
        boolean passed = Boolean.TRUE.equals(checks.get("passed"));
        run.setDraftJson(JsonUtils.toJson(draft))
                .setCheckJson(JsonUtils.toJson(checks))
                .setBlueprintConfirmedAt(LocalDateTime.now())
                .setStatus(passed ? "draft_ready" : "check_failed");
        settingWorkflowRunMapper.updateById(run);
        generationJobService.recordFinishedJob(project.getId(), "setting_workflow_draft", "setting_workflow", run.getId(),
                run.getModelConfigId(), Map.of("blueprint", blueprint), Map.of("draft", draft, "checks", checks));
        return toResponse(run);
    }

    @Override
    @Transactional
    public SettingWorkflowResponse regenerateModule(Long workflowId, String moduleKey) {
        SettingWorkflowRun run = requireRun(workflowId);
        if (!List.of("draft_ready", "check_failed").contains(run.getStatus())) {
            throw new BusinessException(ErrorCode.WORKFLOW_GATE_NOT_MET, "生成草案后才能重生成模块");
        }
        String normalizedModuleKey = normalizeModuleKey(moduleKey);
        Project project = requireProject(run.getProjectId());
        Idea idea = requireSelectedIdea(project, run.getSourceIdeaId());
        Map<String, Object> blueprint = JsonUtils.toMap(run.getBlueprintJson());
        Map<String, Object> currentDraft = JsonUtils.toMap(run.getDraftJson());
        AiGenerateResult result = aiOrchestratorService.generateSettingDraft(run.getModelConfigId(), settingContext(project, idea), blueprint);
        Map<String, Object> regeneratedDraft = requireJsonObject(result.getContent(), "重生成模块不是合法 JSON");
        Object regeneratedModule = regeneratedDraft.get(normalizedModuleKey);
        if (regeneratedModule == null) {
            throw new BusinessException(ErrorCode.AI_TASK_UNAVAILABLE, "模型未返回模块：" + normalizedModuleKey);
        }
        currentDraft.put(normalizedModuleKey, regeneratedModule);
        if ("overview".equals(normalizedModuleKey)) {
            currentDraft.put("overview", text(regeneratedModule));
        }
        Map<String, Object> checks = checkDraft(currentDraft);
        boolean passed = Boolean.TRUE.equals(checks.get("passed"));
        run.setDraftJson(JsonUtils.toJson(currentDraft))
                .setCheckJson(JsonUtils.toJson(checks))
                .setStatus(passed ? "draft_ready" : "check_failed");
        settingWorkflowRunMapper.updateById(run);
        generationJobService.recordFinishedJob(project.getId(), "setting_workflow_regenerate_" + normalizedModuleKey,
                "setting_workflow", run.getId(), run.getModelConfigId(),
                Map.of("moduleKey", normalizedModuleKey, "blueprint", blueprint),
                Map.of("module", regeneratedModule, "checks", checks));
        return toResponse(run);
    }

    @Override
    @Transactional
    public SettingLibraryResponse commitWorkflow(Long workflowId) {
        SettingWorkflowRun run = requireRunForUpdate(workflowId);
        if ("committed".equals(run.getStatus())) {
            return settingLibraryService.getSettingLibrary(run.getProjectId());
        }
        if (!"draft_ready".equals(run.getStatus())) {
            throw new BusinessException(ErrorCode.WORKFLOW_GATE_NOT_MET, "设定草案通过检查后才能提交");
        }
        Map<String, Object> draft = JsonUtils.toMap(run.getDraftJson());
        Map<String, Long> entityIds = new HashMap<>();
        Long projectId = run.getProjectId();

        SettingLibraryGenerateRequest generateRequest = new SettingLibraryGenerateRequest();
        generateRequest.setProjectId(projectId);
        generateRequest.setIdeaId(run.getSourceIdeaId());
        generateRequest.setModelConfigId(run.getModelConfigId());
        generateRequest.setSourceIdeaSummary(text(draft.get("overview")));
        settingLibraryService.generateSettingLibrary(generateRequest);
        SettingLibraryUpdateRequest updateRequest = new SettingLibraryUpdateRequest();
        updateRequest.setSummary(text(draft.get("overview")));
        updateRequest.setOverview(text(draft.get("overview")));
        updateRequest.setChangeNote("提交设定生成工作流草案");
        settingLibraryService.updateSettingLibrary(projectId, updateRequest);

        for (Map<String, Object> item : listOfMaps(draft.get("rules"))) {
            WorldRule rule = new WorldRule().setProjectId(projectId)
                    .setName(requiredText(item, "name", "规则名称不能为空"))
                    .setRuleType(defaultText(item, "ruleType", "general"))
                    .setDescription(text(item.get("description")))
                    .setTriggerCondition(text(item.get("triggerCondition")))
                    .setEffectResult(text(item.get("effectResult")))
                    .setLimitations(text(item.get("limitations")))
                    .setCost(text(item.get("cost")))
                    .setExceptions(text(item.get("exceptions")))
                    .setVisibilityLevel(defaultText(item, "visibilityLevel", "public"))
                    .setImportance(intValue(item.get("importance")))
                    .setExamples(text(item.get("examples")))
                    .setNotes("workflow:" + run.getId());
            worldRuleMapper.insert(rule);
        }
        for (Map<String, Object> item : listOfMaps(draft.get("characters"))) {
            StoryCharacter character = new StoryCharacter().setProjectId(projectId)
                    .setName(requiredText(item, "name", "角色名称不能为空"))
                    .setAlias(List.of())
                    .setRoleType(defaultText(item, "narrativeRole", "supporting"))
                    .setNarrativeRole(defaultText(item, "narrativeRole", "supporting"))
                    .setIdentity(text(item.get("identity")))
                    .setPublicIdentity(text(item.get("publicIdentity")))
                    .setPersonality(text(item.get("personality")))
                    .setMotivation(text(item.get("motivation")))
                    .setBackground(text(item.get("background")))
                    .setCoreGoal(text(item.get("coreGoal")))
                    .setInnerNeed(text(item.get("innerNeed")))
                    .setCoreFlaw(text(item.get("coreFlaw")))
                    .setBottomLine(text(item.get("bottomLine")))
                    .setSkillsSummary(text(item.get("skillsSummary")))
                    .setSecretNotes(text(item.get("secretNotes")))
                    .setRelationshipSummary("")
                    .setImportance(intValue(item.get("importance")))
                    .setStatus("active")
                    .setNotes("workflow:" + run.getId());
            storyCharacterMapper.insert(character);
            entityIds.put(requiredText(item, "key", "角色 key 不能为空"), character.getId());
        }
        for (Map<String, Object> item : listOfMaps(draft.get("organizations"))) {
            Organization organization = new Organization().setProjectId(projectId)
                    .setName(requiredText(item, "name", "组织名称不能为空"))
                    .setOrganizationType(defaultText(item, "organizationType", "faction"))
                    .setPublicMission(text(item.get("publicMission")))
                    .setRealGoal(text(item.get("realGoal")))
                    .setControlledResources(text(item.get("controlledResources")))
                    .setPowerScope(text(item.get("powerScope")))
                    .setEntryRules(text(item.get("entryRules")))
                    .setStatus("active")
                    .setNotes("workflow:" + run.getId());
            organizationMapper.insert(organization);
            entityIds.put(requiredText(item, "key", "组织 key 不能为空"), organization.getId());
        }
        for (Map<String, Object> item : listOfMaps(draft.get("locations"))) {
            StoryLocation location = new StoryLocation().setProjectId(projectId)
                    .setName(requiredText(item, "name", "地点名称不能为空"))
                    .setLocationType(defaultText(item, "locationType", "place"))
                    .setDescription(text(item.get("description")))
                    .setKeyFeatures(text(item.get("keyFeatures")))
                    .setEntryConditions(text(item.get("entryConditions")))
                    .setAvailableResources(text(item.get("availableResources")))
                    .setRiskLevel(defaultText(item, "riskLevel", "medium"))
                    .setRules(text(item.get("rules")))
                    .setNotes("workflow:" + run.getId());
            storyLocationMapper.insert(location);
            entityIds.put(requiredText(item, "key", "地点 key 不能为空"), location.getId());
        }
        for (Map<String, Object> item : listOfMaps(draft.get("items"))) {
            StoryItem storyItem = new StoryItem().setProjectId(projectId)
                    .setName(requiredText(item, "name", "物品名称不能为空"))
                    .setItemType(defaultText(item, "itemType", "item"))
                    .setDescription(text(item.get("description")))
                    .setUsageRules(text(item.get("usageRules")))
                    .setLimitations(text(item.get("limitations")))
                    .setRarity(text(item.get("rarity")))
                    .setStatus(defaultText(item, "status", "available"))
                    .setNotes("workflow:" + run.getId());
            storyItemMapper.insert(storyItem);
            entityIds.put(requiredText(item, "key", "物品 key 不能为空"), storyItem.getId());
        }

        Map<String, Long> eventIds = new HashMap<>();
        for (Map<String, Object> item : listOfMaps(draft.get("events"))) {
            StoryEvent event = new StoryEvent().setProjectId(projectId)
                    .setName(requiredText(item, "name", "事件名称不能为空"))
                    .setEventType(defaultText(item, "eventType", "story"))
                    .setDescription(text(item.get("description")))
                    .setEventTimeText(text(item.get("eventTimeText")))
                    .setLocationId(entityIds.get(text(item.get("locationKey"))))
                    .setIsPlanned(true)
                    .setImportance(intValue(item.get("importance")));
            storyEventMapper.insert(event);
            eventIds.put(requiredText(item, "key", "事件 key 不能为空"), event.getId());
        }
        for (Map<String, Object> item : listOfMaps(draft.get("relations"))) {
            entityRelationMapper.insert(new EntityRelation().setProjectId(projectId)
                    .setSourceType(defaultText(item, "sourceType", "character"))
                    .setSourceId(entityIds.get(requiredText(item, "sourceKey", "关系 sourceKey 不能为空")))
                    .setTargetType(defaultText(item, "targetType", "character"))
                    .setTargetId(entityIds.get(requiredText(item, "targetKey", "关系 targetKey 不能为空")))
                    .setRelationType(defaultText(item, "relationType", "knows"))
                    .setRelationStatus("active")
                    .setVisibilityLevel("public")
                    .setNote(text(item.get("note"))));
        }
        for (Map<String, Object> item : listOfMaps(draft.get("states"))) {
            entityStateRecordMapper.insert(new EntityStateRecord().setProjectId(projectId)
                    .setEntityType(defaultText(item, "entityType", "character"))
                    .setEntityId(entityIds.get(requiredText(item, "entityKey", "状态 entityKey 不能为空")))
                    .setStateType(requiredText(item, "stateType", "状态类型不能为空"))
                    .setOldValue(mapValue(item.get("oldValue")))
                    .setNewValue(mapValue(item.get("newValue")))
                    .setEventId(eventIds.get(text(item.get("eventKey")))));
        }

        run.setStatus("committed").setCommittedAt(LocalDateTime.now());
        settingWorkflowRunMapper.updateById(run);
        return settingLibraryService.getSettingLibrary(projectId);
    }

    private Map<String, Object> settingContext(Project project, Idea idea) {
        return Map.of(
                "projectTitle", text(project.getTitle()),
                "genres", String.join(" + ", project.getGenres() == null ? List.of() : project.getGenres()),
                "platformTarget", text(project.getPlatformTarget()),
                "stylePreference", text(project.getStylePreference()),
                "projectBrief", text(project.getProjectBrief()),
                "ideaTitle", text(idea.getTitle()),
                "ideaWorldview", text(idea.getWorldview()),
                "ideaMainConflict", text(idea.getMainConflict()),
                "ideaSummary", text(idea.getSummary()));
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

    private List<String> validateBlueprint(Map<String, Object> blueprint) {
        List<String> issues = new ArrayList<>();
        requireNonBlank(blueprint, "corePremise", "缺少作品核心前提", issues);
        requireNonBlank(blueprint, "mainConflict", "缺少主线冲突", issues);
        Object entities = blueprint.get("entities");
        if (!(entities instanceof Map<?, ?> entityMap)) {
            issues.add("缺少 entities");
            return issues;
        }
        for (String key : List.of("characters", "organizations", "locations", "items", "events")) {
            if (listOfMaps(entityMap.get(key)).isEmpty()) {
                issues.add("缺少 " + key);
            }
        }
        return issues;
    }

    private Map<String, Object> checkDraft(Map<String, Object> draft) {
        List<String> issues = new ArrayList<>();
        Set<String> keys = new HashSet<>();
        for (String module : List.of("characters", "organizations", "locations", "items")) {
            for (Map<String, Object> item : listOfMaps(draft.get(module))) {
                String key = text(item.get("key"));
                if (key.isBlank()) {
                    issues.add(module + " 存在空 key");
                } else if (!keys.add(key)) {
                    issues.add("重复 key：" + key);
                }
                if (text(item.get("name")).isBlank()) {
                    issues.add(module + " 存在空名称");
                }
            }
        }
        for (Map<String, Object> relation : listOfMaps(draft.get("relations"))) {
            checkReference(keys, relation.get("sourceKey"), "关系 sourceKey 不存在", issues);
            checkReference(keys, relation.get("targetKey"), "关系 targetKey 不存在", issues);
        }
        for (Map<String, Object> event : listOfMaps(draft.get("events"))) {
            String locationKey = text(event.get("locationKey"));
            if (!locationKey.isBlank()) {
                checkReference(keys, locationKey, "事件地点 key 不存在", issues);
            }
        }
        for (Map<String, Object> state : listOfMaps(draft.get("states"))) {
            checkReference(keys, state.get("entityKey"), "状态实体 key 不存在", issues);
        }
        return Map.of("passed", issues.isEmpty(), "issues", issues);
    }

    private String normalizeModuleKey(String moduleKey) {
        String value = text(moduleKey).trim();
        if ("worldRules".equals(value)) {
            value = "rules";
        }
        if ("stateRecords".equals(value)) {
            value = "states";
        }
        if (!List.of("overview", "rules", "characters", "organizations", "locations", "items", "relations", "events", "states")
                .contains(value)) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "不支持的设定模块：" + moduleKey);
        }
        return value;
    }

    private void checkReference(Set<String> keys, Object value, String message, List<String> issues) {
        String key = text(value);
        if (key.isBlank() || !keys.contains(key)) {
            issues.add(message + "：" + key);
        }
    }

    private void requireNonBlank(Map<String, Object> map, String key, String message, List<String> issues) {
        if (text(map.get(key)).isBlank()) {
            issues.add(message);
        }
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

    private Project requireProject(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "作品不存在");
        }
        return project;
    }

    private Idea requireSelectedIdea(Project project, Long ideaId) {
        Long selectedIdeaId = ideaId == null ? project.getSelectedIdeaId() : ideaId;
        if (selectedIdeaId == null) {
            throw new BusinessException(ErrorCode.WORKFLOW_GATE_NOT_MET, "请先选择创意，再生成设定库");
        }
        Idea idea = ideaMapper.selectById(selectedIdeaId);
        if (idea == null || !project.getId().equals(idea.getProjectId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "创意不存在");
        }
        return idea;
    }

    private SettingWorkflowRun requireRun(Long workflowId) {
        SettingWorkflowRun run = settingWorkflowRunMapper.selectById(workflowId);
        if (run == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "设定生成工作流不存在");
        }
        return run;
    }

    private SettingWorkflowRun requireRunForUpdate(Long workflowId) {
        SettingWorkflowRun run = settingWorkflowRunMapper.selectOne(new LambdaQueryWrapper<SettingWorkflowRun>()
                .eq(SettingWorkflowRun::getId, workflowId)
                .last("FOR UPDATE"));
        if (run == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "璁惧畾鐢熸垚宸ヤ綔娴佷笉瀛樺湪");
        }
        return run;
    }

    private SettingWorkflowResponse toResponse(SettingWorkflowRun run) {
        SettingWorkflowResponse response = settingWorkflowConverter.toResponse(run);
        response.setBlueprint(JsonUtils.toMap(run.getBlueprintJson()));
        response.setDraft(JsonUtils.toMap(run.getDraftJson()));
        response.setChecks(JsonUtils.toMap(run.getCheckJson()));
        return response;
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
