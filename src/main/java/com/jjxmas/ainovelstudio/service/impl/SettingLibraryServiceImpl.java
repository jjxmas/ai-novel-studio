package com.jjxmas.ainovelstudio.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jjxmas.ainovelstudio.common.exception.BusinessException;
import com.jjxmas.ainovelstudio.common.exception.ErrorCode;
import com.jjxmas.ainovelstudio.converter.SettingLibraryConverter;
import com.jjxmas.ainovelstudio.mapper.EntityRelationMapper;
import com.jjxmas.ainovelstudio.mapper.EntityStateRecordMapper;
import com.jjxmas.ainovelstudio.mapper.IdeaMapper;
import com.jjxmas.ainovelstudio.mapper.OrganizationMapper;
import com.jjxmas.ainovelstudio.mapper.ProjectMapper;
import com.jjxmas.ainovelstudio.mapper.SettingLibraryMapper;
import com.jjxmas.ainovelstudio.mapper.StoryCharacterMapper;
import com.jjxmas.ainovelstudio.mapper.StoryEventMapper;
import com.jjxmas.ainovelstudio.mapper.StoryItemMapper;
import com.jjxmas.ainovelstudio.mapper.StoryLocationMapper;
import com.jjxmas.ainovelstudio.mapper.WorldRuleMapper;
import com.jjxmas.ainovelstudio.pojo.dto.EntityRelationResponse;
import com.jjxmas.ainovelstudio.pojo.dto.EntityRelationUpsertRequest;
import com.jjxmas.ainovelstudio.pojo.dto.EntityStateRecordResponse;
import com.jjxmas.ainovelstudio.pojo.dto.EntityStateRecordUpsertRequest;
import com.jjxmas.ainovelstudio.pojo.dto.OrganizationResponse;
import com.jjxmas.ainovelstudio.pojo.dto.OrganizationUpsertRequest;
import com.jjxmas.ainovelstudio.pojo.dto.SettingLibraryGenerateRequest;
import com.jjxmas.ainovelstudio.pojo.dto.SettingLibraryResponse;
import com.jjxmas.ainovelstudio.pojo.dto.SettingLibraryRewriteRequest;
import com.jjxmas.ainovelstudio.pojo.dto.SettingLibraryUpdateRequest;
import com.jjxmas.ainovelstudio.pojo.dto.StoryCharacterResponse;
import com.jjxmas.ainovelstudio.pojo.dto.StoryCharacterUpsertRequest;
import com.jjxmas.ainovelstudio.pojo.dto.StoryEventResponse;
import com.jjxmas.ainovelstudio.pojo.dto.StoryEventUpsertRequest;
import com.jjxmas.ainovelstudio.pojo.dto.StoryItemResponse;
import com.jjxmas.ainovelstudio.pojo.dto.StoryItemUpsertRequest;
import com.jjxmas.ainovelstudio.pojo.dto.StoryLocationResponse;
import com.jjxmas.ainovelstudio.pojo.dto.StoryLocationUpsertRequest;
import com.jjxmas.ainovelstudio.pojo.dto.WorldRuleResponse;
import com.jjxmas.ainovelstudio.pojo.dto.WorldRuleUpsertRequest;
import com.jjxmas.ainovelstudio.pojo.entity.EntityRelation;
import com.jjxmas.ainovelstudio.pojo.entity.EntityStateRecord;
import com.jjxmas.ainovelstudio.pojo.entity.Idea;
import com.jjxmas.ainovelstudio.pojo.entity.Organization;
import com.jjxmas.ainovelstudio.pojo.entity.Project;
import com.jjxmas.ainovelstudio.pojo.entity.SettingLibrary;
import com.jjxmas.ainovelstudio.pojo.entity.StoryCharacter;
import com.jjxmas.ainovelstudio.pojo.entity.StoryEvent;
import com.jjxmas.ainovelstudio.pojo.entity.StoryItem;
import com.jjxmas.ainovelstudio.pojo.entity.StoryLocation;
import com.jjxmas.ainovelstudio.pojo.entity.WorldRule;
import com.jjxmas.ainovelstudio.service.GenerationJobService;
import com.jjxmas.ainovelstudio.service.SettingLibraryService;
import com.jjxmas.ainovelstudio.service.VersionService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettingLibraryServiceImpl extends ServiceImpl<SettingLibraryMapper, SettingLibrary> implements SettingLibraryService {

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
    private final GenerationJobService generationJobService;
    private final VersionService versionService;
    private final SettingLibraryConverter settingLibraryConverter;

    public SettingLibraryServiceImpl(
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
            GenerationJobService generationJobService,
            VersionService versionService,
            SettingLibraryConverter settingLibraryConverter) {
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
        this.generationJobService = generationJobService;
        this.versionService = versionService;
        this.settingLibraryConverter = settingLibraryConverter;
    }

    @Override
    @Transactional
    public SettingLibraryResponse generateSettingLibrary(SettingLibraryGenerateRequest request) {
        Project project = requireProject(request.getProjectId());
        Idea selectedIdea = requireSelectedIdea(project, request.getIdeaId());
        String sourceIdeaSummary = defaultText(request.getSourceIdeaSummary(), selectedIdea.getSummary());
        String overview = """
                基于已选创意生成的结构化设定总览。

                创意摘要：
                %s

                建议下一步补全角色、组织、地点、物品、规则、关系、事件和状态记录，再进入大纲阶段。
                """.formatted(sourceIdeaSummary);

        SettingLibrary setting = findByProjectId(project.getId());
        if (setting == null) {
            setting = new SettingLibrary().setProjectId(project.getId());
        }
        setting.setSourceIdeaId(selectedIdea.getId())
                .setSummary(overview)
                .setOverview(overview)
                .setGenreTemplate(project.getPlatformTarget())
                .setStatus("generated")
                .setConfirmedAt(null);
        saveOrUpdate(setting);

        Map<String, Object> snapshot = settingSnapshot(setting, false);
        Long jobId = generationJobService.recordFinishedJob(
                project.getId(),
                "setting_generation",
                "setting_library",
                setting.getId(),
                request.getModelConfigId(),
                Map.of("ideaId", selectedIdea.getId(), "sourceIdeaSummary", sourceIdeaSummary),
                snapshot);
        versionService.recordVersion(
                project.getId(),
                "setting_library",
                setting.getId(),
                snapshot,
                "ai_generate",
                "生成结构化设定库总览",
                request.getModelConfigId(),
                jobId);
        return toResponse(setting);
    }

    @Override
    public SettingLibraryResponse getSettingLibrary(Long projectId) {
        requireProject(projectId);
        return toResponse(requireSetting(projectId));
    }

    @Override
    @Transactional
    public SettingLibraryResponse updateSettingLibrary(Long projectId, SettingLibraryUpdateRequest request) {
        SettingLibrary setting = requireSetting(projectId);
        String overview = defaultText(request.getOverview(), request.getSummary());
        setting.setSummary(defaultText(request.getSummary(), overview))
                .setOverview(overview)
                .setGenreTemplate(defaultText(request.getGenreTemplate(), setting.getGenreTemplate()))
                .setStatus("edited")
                .setConfirmedAt(null);
        updateById(setting);
        versionService.recordVersion(
                projectId,
                "setting_library",
                setting.getId(),
                settingSnapshot(setting, false),
                "user_edit",
                defaultText(request.getChangeNote(), "用户更新设定库总览"),
                null,
                null);
        return toResponse(setting);
    }

    @Override
    @Transactional
    public SettingLibraryResponse updateSettingLibraryById(Long settingLibraryId, SettingLibraryUpdateRequest request) {
        return updateSettingLibrary(requireSettingById(settingLibraryId).getProjectId(), request);
    }

    @Override
    @Transactional
    public SettingLibraryResponse rewriteSettingLibrary(Long projectId, SettingLibraryRewriteRequest request) {
        SettingLibrary setting = requireSetting(projectId);
        String rewrittenOverview = defaultText(setting.getOverview()) + "\n\n[补充说明]\n" + defaultText(request.getInstruction());
        setting.setSummary(rewrittenOverview)
                .setOverview(rewrittenOverview)
                .setStatus("edited")
                .setConfirmedAt(null);
        updateById(setting);

        Map<String, Object> snapshot = settingSnapshot(setting, false);
        Long jobId = generationJobService.recordFinishedJob(
                projectId,
                "setting_rewrite",
                "setting_library",
                setting.getId(),
                request.getModelConfigId(),
                Map.of("instruction", defaultText(request.getInstruction())),
                snapshot);
        versionService.recordVersion(
                projectId,
                "setting_library",
                setting.getId(),
                snapshot,
                "ai_rewrite",
                "根据修改意见补充设定库总览",
                request.getModelConfigId(),
                jobId);
        return toResponse(setting);
    }

    @Override
    @Transactional
    public SettingLibraryResponse confirmSettingLibrary(Long projectId) {
        SettingLibrary setting = requireSetting(projectId);
        setting.setStatus("confirmed").setConfirmedAt(LocalDateTime.now());
        updateById(setting);

        Project project = requireProject(projectId);
        project.setStatus("setting_confirmed");
        projectMapper.updateById(project);

        versionService.recordVersion(
                projectId,
                "setting_library",
                setting.getId(),
                settingSnapshot(setting, true),
                "confirm",
                "确认设定库",
                null,
                null);
        return toResponse(setting);
    }

    @Override
    @Transactional
    public SettingLibraryResponse confirmSettingLibraryById(Long settingLibraryId) {
        return confirmSettingLibrary(requireSettingById(settingLibraryId).getProjectId());
    }

    @Override
    public List<StoryCharacterResponse> listCharacters(Long projectId) {
        requireProject(projectId);
        return settingLibraryConverter.toCharacterResponseList(storyCharacterMapper.selectList(new LambdaQueryWrapper<StoryCharacter>()
                        .eq(StoryCharacter::getProjectId, projectId)
                        .orderByDesc(StoryCharacter::getImportance)
                        .orderByAsc(StoryCharacter::getId)));
    }

    @Override
    @Transactional
    public Long createCharacter(Long projectId, StoryCharacterUpsertRequest request) {
        requireProject(projectId);
        StoryCharacter character = new StoryCharacter().setProjectId(projectId);
        applyCharacter(character, request);
        storyCharacterMapper.insert(character);
        return character.getId();
    }

    @Override
    @Transactional
    public void updateCharacter(Long projectId, Long characterId, StoryCharacterUpsertRequest request) {
        StoryCharacter character = requireCharacter(projectId, characterId);
        applyCharacter(character, request);
        storyCharacterMapper.updateById(character);
    }

    @Override
    @Transactional
    public void deleteCharacter(Long projectId, Long characterId) {
        storyCharacterMapper.deleteById(requireCharacter(projectId, characterId).getId());
    }

    @Override
    public List<OrganizationResponse> listOrganizations(Long projectId) {
        requireProject(projectId);
        return settingLibraryConverter.toOrganizationResponseList(organizationMapper.selectList(new LambdaQueryWrapper<Organization>()
                        .eq(Organization::getProjectId, projectId)
                        .orderByAsc(Organization::getName)));
    }

    @Override
    @Transactional
    public Long createOrganization(Long projectId, OrganizationUpsertRequest request) {
        requireProject(projectId);
        Organization organization = new Organization().setProjectId(projectId);
        applyOrganization(organization, request);
        organizationMapper.insert(organization);
        return organization.getId();
    }

    @Override
    @Transactional
    public void updateOrganization(Long projectId, Long organizationId, OrganizationUpsertRequest request) {
        Organization organization = requireOrganization(projectId, organizationId);
        applyOrganization(organization, request);
        organizationMapper.updateById(organization);
    }

    @Override
    @Transactional
    public void deleteOrganization(Long projectId, Long organizationId) {
        organizationMapper.deleteById(requireOrganization(projectId, organizationId).getId());
    }

    @Override
    public List<StoryLocationResponse> listLocations(Long projectId) {
        requireProject(projectId);
        return settingLibraryConverter.toLocationResponseList(storyLocationMapper.selectList(new LambdaQueryWrapper<StoryLocation>()
                        .eq(StoryLocation::getProjectId, projectId)
                        .orderByAsc(StoryLocation::getParentLocationId)
                        .orderByAsc(StoryLocation::getName)));
    }

    @Override
    @Transactional
    public Long createLocation(Long projectId, StoryLocationUpsertRequest request) {
        requireProject(projectId);
        StoryLocation location = new StoryLocation().setProjectId(projectId);
        applyLocation(location, request);
        storyLocationMapper.insert(location);
        return location.getId();
    }

    @Override
    @Transactional
    public void updateLocation(Long projectId, Long locationId, StoryLocationUpsertRequest request) {
        StoryLocation location = requireLocation(projectId, locationId);
        applyLocation(location, request);
        storyLocationMapper.updateById(location);
    }

    @Override
    @Transactional
    public void deleteLocation(Long projectId, Long locationId) {
        storyLocationMapper.deleteById(requireLocation(projectId, locationId).getId());
    }

    @Override
    public List<StoryItemResponse> listItems(Long projectId) {
        requireProject(projectId);
        return settingLibraryConverter.toItemResponseList(storyItemMapper.selectList(new LambdaQueryWrapper<StoryItem>()
                        .eq(StoryItem::getProjectId, projectId)
                        .orderByAsc(StoryItem::getName)));
    }

    @Override
    @Transactional
    public Long createItem(Long projectId, StoryItemUpsertRequest request) {
        requireProject(projectId);
        StoryItem item = new StoryItem().setProjectId(projectId);
        applyItem(item, request);
        storyItemMapper.insert(item);
        return item.getId();
    }

    @Override
    @Transactional
    public void updateItem(Long projectId, Long itemId, StoryItemUpsertRequest request) {
        StoryItem item = requireItem(projectId, itemId);
        applyItem(item, request);
        storyItemMapper.updateById(item);
    }

    @Override
    @Transactional
    public void deleteItem(Long projectId, Long itemId) {
        storyItemMapper.deleteById(requireItem(projectId, itemId).getId());
    }

    @Override
    public List<WorldRuleResponse> listWorldRules(Long projectId) {
        requireProject(projectId);
        return settingLibraryConverter.toWorldRuleResponseList(worldRuleMapper.selectList(new LambdaQueryWrapper<WorldRule>()
                        .eq(WorldRule::getProjectId, projectId)
                        .orderByDesc(WorldRule::getImportance)
                        .orderByAsc(WorldRule::getName)));
    }

    @Override
    @Transactional
    public Long createWorldRule(Long projectId, WorldRuleUpsertRequest request) {
        requireProject(projectId);
        WorldRule worldRule = new WorldRule().setProjectId(projectId);
        applyWorldRule(worldRule, request);
        worldRuleMapper.insert(worldRule);
        return worldRule.getId();
    }

    @Override
    @Transactional
    public void updateWorldRule(Long projectId, Long ruleId, WorldRuleUpsertRequest request) {
        WorldRule worldRule = requireWorldRule(projectId, ruleId);
        applyWorldRule(worldRule, request);
        worldRuleMapper.updateById(worldRule);
    }

    @Override
    @Transactional
    public void deleteWorldRule(Long projectId, Long ruleId) {
        worldRuleMapper.deleteById(requireWorldRule(projectId, ruleId).getId());
    }

    @Override
    public List<EntityRelationResponse> listRelations(Long projectId) {
        requireProject(projectId);
        return settingLibraryConverter.toRelationResponseList(entityRelationMapper.selectList(new LambdaQueryWrapper<EntityRelation>()
                        .eq(EntityRelation::getProjectId, projectId)
                        .orderByAsc(EntityRelation::getSourceType)
                        .orderByAsc(EntityRelation::getSourceId)
                        .orderByAsc(EntityRelation::getRelationType)));
    }

    @Override
    @Transactional
    public Long createRelation(Long projectId, EntityRelationUpsertRequest request) {
        requireProject(projectId);
        EntityRelation relation = new EntityRelation().setProjectId(projectId);
        applyRelation(relation, request);
        entityRelationMapper.insert(relation);
        return relation.getId();
    }

    @Override
    @Transactional
    public void updateRelation(Long projectId, Long relationId, EntityRelationUpsertRequest request) {
        EntityRelation relation = requireRelation(projectId, relationId);
        applyRelation(relation, request);
        entityRelationMapper.updateById(relation);
    }

    @Override
    @Transactional
    public void deleteRelation(Long projectId, Long relationId) {
        entityRelationMapper.deleteById(requireRelation(projectId, relationId).getId());
    }

    @Override
    public List<StoryEventResponse> listEvents(Long projectId) {
        requireProject(projectId);
        return settingLibraryConverter.toEventResponseList(storyEventMapper.selectList(new LambdaQueryWrapper<StoryEvent>()
                        .eq(StoryEvent::getProjectId, projectId)
                        .orderByDesc(StoryEvent::getImportance)
                        .orderByAsc(StoryEvent::getId)));
    }

    @Override
    @Transactional
    public Long createEvent(Long projectId, StoryEventUpsertRequest request) {
        requireProject(projectId);
        StoryEvent event = new StoryEvent().setProjectId(projectId);
        applyEvent(event, request);
        storyEventMapper.insert(event);
        return event.getId();
    }

    @Override
    @Transactional
    public void updateEvent(Long projectId, Long eventId, StoryEventUpsertRequest request) {
        StoryEvent event = requireEvent(projectId, eventId);
        applyEvent(event, request);
        storyEventMapper.updateById(event);
    }

    @Override
    @Transactional
    public void deleteEvent(Long projectId, Long eventId) {
        storyEventMapper.deleteById(requireEvent(projectId, eventId).getId());
    }

    @Override
    public List<EntityStateRecordResponse> listStateRecords(Long projectId) {
        requireProject(projectId);
        return settingLibraryConverter.toStateRecordResponseList(entityStateRecordMapper.selectList(new LambdaQueryWrapper<EntityStateRecord>()
                        .eq(EntityStateRecord::getProjectId, projectId)
                        .orderByDesc(EntityStateRecord::getCreatedAt)));
    }

    @Override
    @Transactional
    public Long createStateRecord(Long projectId, EntityStateRecordUpsertRequest request) {
        requireProject(projectId);
        EntityStateRecord record = new EntityStateRecord().setProjectId(projectId);
        applyStateRecord(record, request);
        entityStateRecordMapper.insert(record);
        return record.getId();
    }

    @Override
    @Transactional
    public void updateStateRecord(Long projectId, Long recordId, EntityStateRecordUpsertRequest request) {
        EntityStateRecord record = requireStateRecord(projectId, recordId);
        applyStateRecord(record, request);
        entityStateRecordMapper.updateById(record);
    }

    @Override
    @Transactional
    public void deleteStateRecord(Long projectId, Long recordId) {
        entityStateRecordMapper.deleteById(requireStateRecord(projectId, recordId).getId());
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

    private SettingLibrary requireSetting(Long projectId) {
        SettingLibrary setting = findByProjectId(projectId);
        if (setting == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "设定库不存在");
        }
        return setting;
    }

    private SettingLibrary requireSettingById(Long settingLibraryId) {
        SettingLibrary setting = getById(settingLibraryId);
        if (setting == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "设定库不存在");
        }
        return setting;
    }

    private StoryCharacter requireCharacter(Long projectId, Long characterId) {
        StoryCharacter character = storyCharacterMapper.selectById(characterId);
        if (character == null || !projectId.equals(character.getProjectId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        return character;
    }

    private Organization requireOrganization(Long projectId, Long organizationId) {
        Organization organization = organizationMapper.selectById(organizationId);
        if (organization == null || !projectId.equals(organization.getProjectId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "组织不存在");
        }
        return organization;
    }

    private StoryLocation requireLocation(Long projectId, Long locationId) {
        StoryLocation location = storyLocationMapper.selectById(locationId);
        if (location == null || !projectId.equals(location.getProjectId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "地点不存在");
        }
        return location;
    }

    private StoryItem requireItem(Long projectId, Long itemId) {
        StoryItem item = storyItemMapper.selectById(itemId);
        if (item == null || !projectId.equals(item.getProjectId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "物品不存在");
        }
        return item;
    }

    private WorldRule requireWorldRule(Long projectId, Long ruleId) {
        WorldRule worldRule = worldRuleMapper.selectById(ruleId);
        if (worldRule == null || !projectId.equals(worldRule.getProjectId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "规则不存在");
        }
        return worldRule;
    }

    private EntityRelation requireRelation(Long projectId, Long relationId) {
        EntityRelation relation = entityRelationMapper.selectById(relationId);
        if (relation == null || !projectId.equals(relation.getProjectId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "关系不存在");
        }
        return relation;
    }

    private StoryEvent requireEvent(Long projectId, Long eventId) {
        StoryEvent event = storyEventMapper.selectById(eventId);
        if (event == null || !projectId.equals(event.getProjectId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "事件不存在");
        }
        return event;
    }

    private EntityStateRecord requireStateRecord(Long projectId, Long recordId) {
        EntityStateRecord record = entityStateRecordMapper.selectById(recordId);
        if (record == null || !projectId.equals(record.getProjectId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "状态记录不存在");
        }
        return record;
    }

    private SettingLibrary findByProjectId(Long projectId) {
        requireProject(projectId);
        return getOne(new LambdaQueryWrapper<SettingLibrary>()
                .eq(SettingLibrary::getProjectId, projectId)
                .last("LIMIT 1"));
    }

    private void applyCharacter(StoryCharacter character, StoryCharacterUpsertRequest request) {
        settingLibraryConverter.updateCharacter(request, character);
        character
                .setRoleType(defaultText(request.getRoleType(), "supporting"))
                .setNarrativeRole(defaultText(request.getNarrativeRole(), "supporting"))
                .setIdentity(request.getIdentity())
                .setPublicIdentity(request.getPublicIdentity())
                .setGender(request.getGender())
                .setAgeText(request.getAgeText())
                .setPersonality(request.getPersonality())
                .setMotivation(request.getMotivation())
                .setBackground(request.getBackground())
                .setCoreGoal(request.getCoreGoal())
                .setInnerNeed(request.getInnerNeed())
                .setCoreFlaw(request.getCoreFlaw())
                .setBottomLine(request.getBottomLine())
                .setSkillsSummary(request.getSkillsSummary())
                .setSecretNotes(request.getSecretNotes())
                .setRelationshipSummary(request.getRelationshipSummary())
                .setImportance(request.getImportance() == null ? 0 : request.getImportance())
                .setStatus(defaultText(request.getStatus(), "active"))
                .setFirstAppearedChapterId(request.getFirstAppearedChapterId())
                .setNotes(request.getNotes());
    }

    private void applyOrganization(Organization organization, OrganizationUpsertRequest request) {
        settingLibraryConverter.updateOrganization(request, organization);
        organization
                .setOrganizationType(defaultText(request.getOrganizationType(), "faction"))
                .setStatus(defaultText(request.getStatus(), "active"));
    }

    private void applyLocation(StoryLocation location, StoryLocationUpsertRequest request) {
        settingLibraryConverter.updateLocation(request, location);
        location
                .setLocationType(defaultText(request.getLocationType(), "place"))
                .setRiskLevel(defaultText(request.getRiskLevel(), "medium"));
    }

    private void applyItem(StoryItem item, StoryItemUpsertRequest request) {
        settingLibraryConverter.updateItem(request, item);
        item
                .setItemType(defaultText(request.getItemType(), "item"))
                .setStatus(defaultText(request.getStatus(), "available"));
    }

    private void applyWorldRule(WorldRule worldRule, WorldRuleUpsertRequest request) {
        settingLibraryConverter.updateWorldRule(request, worldRule);
        worldRule
                .setRuleType(defaultText(request.getRuleType(), "general"))
                .setVisibilityLevel(defaultText(request.getVisibilityLevel(), "public"))
                .setImportance(request.getImportance() == null ? 0 : request.getImportance());
    }

    private void applyRelation(EntityRelation relation, EntityRelationUpsertRequest request) {
        settingLibraryConverter.updateRelation(request, relation);
        relation
                .setRelationStatus(defaultText(request.getRelationStatus(), "active"))
                .setVisibilityLevel(defaultText(request.getVisibilityLevel(), "public"));
    }

    private void applyEvent(StoryEvent event, StoryEventUpsertRequest request) {
        settingLibraryConverter.updateEvent(request, event);
        event
                .setEventType(defaultText(request.getEventType(), "story"))
                .setIsPlanned(request.getPlanned() == null ? Boolean.TRUE : request.getPlanned())
                .setImportance(request.getImportance() == null ? 0 : request.getImportance());
    }

    private void applyStateRecord(EntityStateRecord record, EntityStateRecordUpsertRequest request) {
        settingLibraryConverter.updateStateRecord(request, record);
    }

    private SettingLibraryResponse toResponse(SettingLibrary setting) {
        int characterCount = count(storyCharacterMapper.selectCount(new LambdaQueryWrapper<StoryCharacter>()
                .eq(StoryCharacter::getProjectId, setting.getProjectId())));
        int organizationCount = count(organizationMapper.selectCount(new LambdaQueryWrapper<Organization>()
                .eq(Organization::getProjectId, setting.getProjectId())));
        int locationCount = count(storyLocationMapper.selectCount(new LambdaQueryWrapper<StoryLocation>()
                .eq(StoryLocation::getProjectId, setting.getProjectId())));
        int itemCount = count(storyItemMapper.selectCount(new LambdaQueryWrapper<StoryItem>()
                .eq(StoryItem::getProjectId, setting.getProjectId())));
        int ruleCount = count(worldRuleMapper.selectCount(new LambdaQueryWrapper<WorldRule>()
                .eq(WorldRule::getProjectId, setting.getProjectId())));
        int relationCount = count(entityRelationMapper.selectCount(new LambdaQueryWrapper<EntityRelation>()
                .eq(EntityRelation::getProjectId, setting.getProjectId())));
        int eventCount = count(storyEventMapper.selectCount(new LambdaQueryWrapper<StoryEvent>()
                .eq(StoryEvent::getProjectId, setting.getProjectId())));
        int stateRecordCount = count(entityStateRecordMapper.selectCount(new LambdaQueryWrapper<EntityStateRecord>()
                .eq(EntityStateRecord::getProjectId, setting.getProjectId())));

        return SettingLibraryResponse.builder()
                .id(setting.getId())
                .projectId(setting.getProjectId())
                .sourceIdeaId(setting.getSourceIdeaId())
                .summary(defaultText(setting.getSummary()))
                .overview(defaultText(setting.getOverview(), setting.getSummary()))
                .genreTemplate(setting.getGenreTemplate())
                .status(defaultText(setting.getStatus(), "draft"))
                .confirmed(setting.getConfirmedAt() != null)
                .confirmedAt(setting.getConfirmedAt())
                .characterCount(characterCount)
                .organizationCount(organizationCount)
                .locationCount(locationCount)
                .itemCount(itemCount)
                .ruleCount(ruleCount)
                .relationCount(relationCount)
                .eventCount(eventCount)
                .stateRecordCount(stateRecordCount)
                .completenessScore(completenessScore(
                        characterCount,
                        organizationCount,
                        locationCount,
                        itemCount,
                        ruleCount,
                        relationCount,
                        eventCount,
                        stateRecordCount))
                .build();
    }

    private Map<String, Object> settingSnapshot(SettingLibrary setting, boolean confirmed) {
        return Map.of(
                "summary", defaultText(setting.getSummary()),
                "overview", defaultText(setting.getOverview(), setting.getSummary()),
                "genreTemplate", defaultText(setting.getGenreTemplate()),
                "status", defaultText(setting.getStatus(), "draft"),
                "confirmed", confirmed);
    }

    private int count(Long value) {
        return value == null ? 0 : value.intValue();
    }

    private int completenessScore(
            int characterCount,
            int organizationCount,
            int locationCount,
            int itemCount,
            int ruleCount,
            int relationCount,
            int eventCount,
            int stateRecordCount) {
        int filled = 0;
        if (characterCount > 0) {
            filled += 1;
        }
        if (organizationCount > 0) {
            filled += 1;
        }
        if (locationCount > 0) {
            filled += 1;
        }
        if (itemCount > 0) {
            filled += 1;
        }
        if (ruleCount > 0) {
            filled += 1;
        }
        if (relationCount > 0) {
            filled += 1;
        }
        if (eventCount > 0) {
            filled += 1;
        }
        if (stateRecordCount > 0) {
            filled += 1;
        }
        return (int) Math.round(filled * 100.0 / 8.0);
    }

    private String defaultText(String value) {
        return value == null ? "" : value;
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? defaultText(fallback) : value;
    }
}
