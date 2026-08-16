package com.jjxmas.ainovelstudio.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jjxmas.ainovelstudio.common.exception.BusinessException;
import com.jjxmas.ainovelstudio.common.exception.ErrorCode;
import com.jjxmas.ainovelstudio.common.entity.BaseEntity;
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
import com.jjxmas.ainovelstudio.pojo.dto.SettingLibraryResponse;
import com.jjxmas.ainovelstudio.pojo.dto.SettingLibraryRewriteRequest;
import com.jjxmas.ainovelstudio.pojo.dto.SettingLibrarySnapshotResponse;
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
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
    private final CacheManager cacheManager;

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
            SettingLibraryConverter settingLibraryConverter,
            CacheManager cacheManager) {
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
        this.cacheManager = cacheManager;
    }

    @Override
    @Transactional
    @CacheEvict(value = {"settingLibraries", "chapterContextSettings"}, key = "#projectId")
    public SettingLibraryResponse commitWorkflowDraft(
            Long projectId,
            Long sourceIdeaId,
            String overview,
            Long modelConfigId,
            Long workflowId) {
        Project project = requireProject(projectId);
        Idea selectedIdea = requireSelectedIdea(project, sourceIdeaId);
        String normalizedOverview = defaultText(overview, selectedIdea.getSummary());
        SettingLibrary setting = findByProjectId(project.getId());
        if (setting == null) {
            setting = new SettingLibrary().setProjectId(project.getId());
        }
        setting.setSourceIdeaId(selectedIdea.getId())
                .setSummary(normalizedOverview)
                .setOverview(normalizedOverview)
                .setGenreTemplate(project.getPlatformTarget())
                .setStatus("generated")
                .setConfirmedAt(null);
        saveOrUpdate(setting);

        Map<String, Object> snapshot = settingSnapshot(setting, false);
        versionService.recordVersion(
                project.getId(),
                "setting_library",
                setting.getId(),
                snapshot,
                "ai_generate",
                "提交设定工作流 #" + workflowId,
                modelConfigId,
                null);
        evictSettingLibrary(projectId);
        return toResponse(setting);
    }

    @Override
    @Cacheable(value = "settingLibraries", key = "#projectId")
    public SettingLibraryResponse getSettingLibrary(Long projectId) {
        requireProject(projectId);
        return toResponse(requireSetting(projectId));
    }

    @Override
    public SettingLibrarySnapshotResponse getSettingLibrarySnapshot(Long projectId) {
        requireProject(projectId);
        SettingLibrary setting = requireSetting(projectId);
        List<StoryCharacterResponse> characters = listCharacters(projectId);
        List<OrganizationResponse> organizations = listOrganizations(projectId);
        List<StoryLocationResponse> locations = listLocations(projectId);
        List<StoryItemResponse> items = listItems(projectId);
        List<WorldRuleResponse> worldRules = listWorldRules(projectId);
        List<EntityRelationResponse> relations = listRelations(projectId);
        List<StoryEventResponse> events = listEvents(projectId);
        List<EntityStateRecordResponse> stateRecords = listStateRecords(projectId);
        return SettingLibrarySnapshotResponse.builder()
                .settingLibrary(toResponse(
                        setting,
                        characters.size(),
                        organizations.size(),
                        locations.size(),
                        items.size(),
                        worldRules.size(),
                        relations.size(),
                        events.size(),
                        stateRecords.size()))
                .characters(characters)
                .organizations(organizations)
                .locations(locations)
                .items(items)
                .worldRules(worldRules)
                .relations(relations)
                .events(events)
                .stateRecords(stateRecords)
                .build();
    }

    @Override
    @Transactional
    @CacheEvict(value = {"settingLibraries", "chapterContextSettings"}, key = "#projectId")
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
        evictSettingLibrary(projectId);
        return toResponse(setting);
    }

    @Override
    @Transactional
    public SettingLibraryResponse updateSettingLibraryById(Long settingLibraryId, SettingLibraryUpdateRequest request) {
        Long projectId = requireSettingById(settingLibraryId).getProjectId();
        SettingLibraryResponse response = updateSettingLibrary(projectId, request);
        evictSettingLibrary(projectId);
        return response;
    }

    @Override
    @Transactional
    @CacheEvict(value = {"settingLibraries", "chapterContextSettings"}, key = "#projectId")
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
    @CacheEvict(value = {"settingLibraries", "chapterContextSettings"}, key = "#projectId")
    public SettingLibraryResponse confirmSettingLibrary(Long projectId) {
        SettingLibrary setting = requireSetting(projectId);
        setting.setStatus("confirmed").setConfirmedAt(LocalDateTime.now());
        updateById(setting);

        Project project = requireProject(projectId);
        project.setWorkflowStage("outline");
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
        evictSettingLibrary(projectId);
        return toResponse(setting);
    }

    @Override
    @Transactional
    public SettingLibraryResponse confirmSettingLibraryById(Long settingLibraryId) {
        Long projectId = requireSettingById(settingLibraryId).getProjectId();
        SettingLibraryResponse response = confirmSettingLibrary(projectId);
        evictSettingLibrary(projectId);
        return response;
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
    public StoryCharacterResponse createCharacter(Long projectId, StoryCharacterUpsertRequest request) {
        return createEntity(
                projectId,
                new StoryCharacter().setProjectId(projectId),
                request,
                this::applyCharacter,
                storyCharacterMapper,
                settingLibraryConverter::toCharacterResponse);
    }

    @Override
    @Transactional
    public StoryCharacterResponse updateCharacter(Long projectId, Long characterId, StoryCharacterUpsertRequest request) {
        StoryCharacter character = requireCharacter(projectId, characterId);
        return updateEntity(projectId, character, request, this::applyCharacter, storyCharacterMapper, settingLibraryConverter::toCharacterResponse);
    }

    @Override
    @Transactional
    public void deleteCharacter(Long projectId, Long characterId) {
        storyCharacterMapper.deleteById(requireCharacter(projectId, characterId).getId());
        evictSettingLibrary(projectId);
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
    public OrganizationResponse createOrganization(Long projectId, OrganizationUpsertRequest request) {
        return createEntity(
                projectId,
                new Organization().setProjectId(projectId),
                request,
                this::applyOrganization,
                organizationMapper,
                settingLibraryConverter::toOrganizationResponse);
    }

    @Override
    @Transactional
    public OrganizationResponse updateOrganization(Long projectId, Long organizationId, OrganizationUpsertRequest request) {
        Organization organization = requireOrganization(projectId, organizationId);
        return updateEntity(projectId, organization, request, this::applyOrganization, organizationMapper, settingLibraryConverter::toOrganizationResponse);
    }

    @Override
    @Transactional
    public void deleteOrganization(Long projectId, Long organizationId) {
        organizationMapper.deleteById(requireOrganization(projectId, organizationId).getId());
        evictSettingLibrary(projectId);
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
    public StoryLocationResponse createLocation(Long projectId, StoryLocationUpsertRequest request) {
        return createEntity(
                projectId,
                new StoryLocation().setProjectId(projectId),
                request,
                this::applyLocation,
                storyLocationMapper,
                settingLibraryConverter::toLocationResponse);
    }

    @Override
    @Transactional
    public StoryLocationResponse updateLocation(Long projectId, Long locationId, StoryLocationUpsertRequest request) {
        StoryLocation location = requireLocation(projectId, locationId);
        return updateEntity(projectId, location, request, this::applyLocation, storyLocationMapper, settingLibraryConverter::toLocationResponse);
    }

    @Override
    @Transactional
    public void deleteLocation(Long projectId, Long locationId) {
        storyLocationMapper.deleteById(requireLocation(projectId, locationId).getId());
        evictSettingLibrary(projectId);
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
    public StoryItemResponse createItem(Long projectId, StoryItemUpsertRequest request) {
        return createEntity(
                projectId,
                new StoryItem().setProjectId(projectId),
                request,
                this::applyItem,
                storyItemMapper,
                settingLibraryConverter::toItemResponse);
    }

    @Override
    @Transactional
    public StoryItemResponse updateItem(Long projectId, Long itemId, StoryItemUpsertRequest request) {
        StoryItem item = requireItem(projectId, itemId);
        return updateEntity(projectId, item, request, this::applyItem, storyItemMapper, settingLibraryConverter::toItemResponse);
    }

    @Override
    @Transactional
    public void deleteItem(Long projectId, Long itemId) {
        storyItemMapper.deleteById(requireItem(projectId, itemId).getId());
        evictSettingLibrary(projectId);
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
    public WorldRuleResponse createWorldRule(Long projectId, WorldRuleUpsertRequest request) {
        return createEntity(
                projectId,
                new WorldRule().setProjectId(projectId),
                request,
                this::applyWorldRule,
                worldRuleMapper,
                settingLibraryConverter::toWorldRuleResponse);
    }

    @Override
    @Transactional
    public WorldRuleResponse updateWorldRule(Long projectId, Long ruleId, WorldRuleUpsertRequest request) {
        WorldRule worldRule = requireWorldRule(projectId, ruleId);
        return updateEntity(projectId, worldRule, request, this::applyWorldRule, worldRuleMapper, settingLibraryConverter::toWorldRuleResponse);
    }

    @Override
    @Transactional
    public void deleteWorldRule(Long projectId, Long ruleId) {
        worldRuleMapper.deleteById(requireWorldRule(projectId, ruleId).getId());
        evictSettingLibrary(projectId);
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
    public EntityRelationResponse createRelation(Long projectId, EntityRelationUpsertRequest request) {
        return createEntity(
                projectId,
                new EntityRelation().setProjectId(projectId),
                request,
                this::applyRelation,
                entityRelationMapper,
                settingLibraryConverter::toRelationResponse);
    }

    @Override
    @Transactional
    public EntityRelationResponse updateRelation(Long projectId, Long relationId, EntityRelationUpsertRequest request) {
        EntityRelation relation = requireRelation(projectId, relationId);
        return updateEntity(projectId, relation, request, this::applyRelation, entityRelationMapper, settingLibraryConverter::toRelationResponse);
    }

    @Override
    @Transactional
    public void deleteRelation(Long projectId, Long relationId) {
        entityRelationMapper.deleteById(requireRelation(projectId, relationId).getId());
        evictSettingLibrary(projectId);
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
    public StoryEventResponse createEvent(Long projectId, StoryEventUpsertRequest request) {
        return createEntity(
                projectId,
                new StoryEvent().setProjectId(projectId),
                request,
                this::applyEvent,
                storyEventMapper,
                settingLibraryConverter::toEventResponse);
    }

    @Override
    @Transactional
    public StoryEventResponse updateEvent(Long projectId, Long eventId, StoryEventUpsertRequest request) {
        StoryEvent event = requireEvent(projectId, eventId);
        return updateEntity(projectId, event, request, this::applyEvent, storyEventMapper, settingLibraryConverter::toEventResponse);
    }

    @Override
    @Transactional
    public void deleteEvent(Long projectId, Long eventId) {
        storyEventMapper.deleteById(requireEvent(projectId, eventId).getId());
        evictSettingLibrary(projectId);
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
    public EntityStateRecordResponse createStateRecord(Long projectId, EntityStateRecordUpsertRequest request) {
        return createEntity(
                projectId,
                new EntityStateRecord().setProjectId(projectId),
                request,
                this::applyStateRecord,
                entityStateRecordMapper,
                settingLibraryConverter::toStateRecordResponse);
    }

    @Override
    @Transactional
    public EntityStateRecordResponse updateStateRecord(Long projectId, Long recordId, EntityStateRecordUpsertRequest request) {
        EntityStateRecord record = requireStateRecord(projectId, recordId);
        return updateEntity(projectId, record, request, this::applyStateRecord, entityStateRecordMapper, settingLibraryConverter::toStateRecordResponse);
    }

    @Override
    @Transactional
    public void deleteStateRecord(Long projectId, Long recordId) {
        entityStateRecordMapper.deleteById(requireStateRecord(projectId, recordId).getId());
        evictSettingLibrary(projectId);
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

    private <E extends BaseEntity, Q, R> R createEntity(
            Long projectId,
            E entity,
            Q request,
            BiConsumer<E, Q> applier,
            BaseMapper<E> mapper,
            Function<E, R> converter) {
        requireProject(projectId);
        applier.accept(entity, request);
        mapper.insert(entity);
        evictSettingLibrary(projectId);
        return converter.apply(entity);
    }

    private <E extends BaseEntity, Q, R> R updateEntity(
            Long projectId,
            E entity,
            Q request,
            BiConsumer<E, Q> applier,
            BaseMapper<E> mapper,
            Function<E, R> converter) {
        applier.accept(entity, request);
        mapper.updateById(entity);
        evictSettingLibrary(projectId);
        return converter.apply(entity);
    }

    private void evictSettingLibrary(Long projectId) {
        for (String cacheName : List.of("settingLibraries", "chapterContextSettings")) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.evict(projectId);
            }
        }
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

        return toResponse(
                setting,
                characterCount,
                organizationCount,
                locationCount,
                itemCount,
                ruleCount,
                relationCount,
                eventCount,
                stateRecordCount);
    }

    private SettingLibraryResponse toResponse(
            SettingLibrary setting,
            int characterCount,
            int organizationCount,
            int locationCount,
            int itemCount,
            int ruleCount,
            int relationCount,
            int eventCount,
            int stateRecordCount) {
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
