package com.jjxmas.ainovelstudio.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jjxmas.ainovelstudio.mapper.EntityRelationMapper;
import com.jjxmas.ainovelstudio.mapper.EntityStateRecordMapper;
import com.jjxmas.ainovelstudio.mapper.OrganizationMapper;
import com.jjxmas.ainovelstudio.mapper.StoryCharacterMapper;
import com.jjxmas.ainovelstudio.mapper.StoryItemMapper;
import com.jjxmas.ainovelstudio.mapper.StoryLocationMapper;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterContext;
import com.jjxmas.ainovelstudio.pojo.entity.Chapter;
import com.jjxmas.ainovelstudio.pojo.entity.EntityRelation;
import com.jjxmas.ainovelstudio.pojo.entity.EntityStateRecord;
import com.jjxmas.ainovelstudio.pojo.entity.Organization;
import com.jjxmas.ainovelstudio.pojo.entity.StoryCharacter;
import com.jjxmas.ainovelstudio.pojo.entity.StoryItem;
import com.jjxmas.ainovelstudio.pojo.entity.StoryLocation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class StoryStateSnapshotService {

    private static final int MAX_CHARACTERS = 5;
    private static final int MAX_ORGANIZATIONS = 3;
    private static final int MAX_LOCATIONS = 3;
    private static final int MAX_ITEMS = 3;
    private static final int MAX_RELATIONS = 6;
    private static final int MAX_STATE_RECORDS = 6;

    private final StoryCharacterMapper storyCharacterMapper;
    private final OrganizationMapper organizationMapper;
    private final StoryLocationMapper storyLocationMapper;
    private final StoryItemMapper storyItemMapper;
    private final EntityRelationMapper entityRelationMapper;
    private final EntityStateRecordMapper entityStateRecordMapper;

    public StoryStateSnapshotService(
            StoryCharacterMapper storyCharacterMapper,
            OrganizationMapper organizationMapper,
            StoryLocationMapper storyLocationMapper,
            StoryItemMapper storyItemMapper,
            EntityRelationMapper entityRelationMapper,
            EntityStateRecordMapper entityStateRecordMapper) {
        this.storyCharacterMapper = storyCharacterMapper;
        this.organizationMapper = organizationMapper;
        this.storyLocationMapper = storyLocationMapper;
        this.storyItemMapper = storyItemMapper;
        this.entityRelationMapper = entityRelationMapper;
        this.entityStateRecordMapper = entityStateRecordMapper;
    }

    public ChapterContext.CurrentState snapshotForChapter(
            Chapter chapter,
            String title,
            String outline,
            List<String> scenePlan,
            String previousSummary) {
        String referenceText = normalizeReference(title, outline, scenePlan, previousSummary);
        List<StoryCharacter> characters = selectCharacters(chapter.getProjectId(), referenceText);
        List<Organization> organizations = selectOrganizations(chapter.getProjectId(), referenceText);
        List<StoryLocation> locations = selectLocations(chapter.getProjectId(), referenceText);
        List<StoryItem> items = selectItems(chapter.getProjectId(), referenceText);
        List<EntityRelation> relations = selectRelations(
                chapter.getProjectId(),
                characters,
                organizations,
                locations,
                items);
        List<EntityStateRecord> stateRecords = selectStateRecords(
                chapter.getProjectId(),
                characters,
                organizations,
                locations,
                items);

        Map<Long, String> characterNames = characters.stream()
                .collect(Collectors.toMap(StoryCharacter::getId, StoryCharacter::getName, (left, right) -> left, LinkedHashMap::new));
        Map<Long, String> organizationNames = organizations.stream()
                .collect(Collectors.toMap(Organization::getId, Organization::getName, (left, right) -> left, LinkedHashMap::new));
        Map<Long, String> locationNames = locations.stream()
                .collect(Collectors.toMap(StoryLocation::getId, StoryLocation::getName, (left, right) -> left, LinkedHashMap::new));
        Map<Long, String> itemNames = items.stream()
                .collect(Collectors.toMap(StoryItem::getId, StoryItem::getName, (left, right) -> left, LinkedHashMap::new));

        return ChapterContext.CurrentState.builder()
                .relevantCharacters(characters.stream().map(this::characterText).toList())
                .relevantOrganizations(organizations.stream().map(this::organizationText).toList())
                .relevantLocations(locations.stream().map(this::locationText).toList())
                .relevantItems(items.stream().map(item -> itemText(item, characterNames, organizationNames)).toList())
                .relevantRelations(relations.stream()
                        .map(relation -> relationText(relation, characterNames, organizationNames, locationNames, itemNames))
                        .toList())
                .relevantStateRecords(stateRecords.stream()
                        .map(record -> stateRecordText(record, characterNames, organizationNames, locationNames, itemNames))
                        .toList())
                .build();
    }

    private List<StoryCharacter> selectCharacters(Long projectId, String referenceText) {
        List<StoryCharacter> all = storyCharacterMapper.selectList(new LambdaQueryWrapper<StoryCharacter>()
                .eq(StoryCharacter::getProjectId, projectId)
                .ne(StoryCharacter::getStatus, "archived")
                .orderByDesc(StoryCharacter::getImportance)
                .orderByAsc(StoryCharacter::getId));
        return selectByReference(all, MAX_CHARACTERS, item -> matchesCharacter(item, referenceText));
    }

    private List<Organization> selectOrganizations(Long projectId, String referenceText) {
        List<Organization> all = organizationMapper.selectList(new LambdaQueryWrapper<Organization>()
                .eq(Organization::getProjectId, projectId)
                .ne(Organization::getStatus, "archived")
                .orderByAsc(Organization::getId));
        return selectByReference(all, MAX_ORGANIZATIONS, item ->
                contains(referenceText, item.getName())
                        || contains(referenceText, item.getPublicMission())
                        || contains(referenceText, item.getRealGoal()));
    }

    private List<StoryLocation> selectLocations(Long projectId, String referenceText) {
        List<StoryLocation> all = storyLocationMapper.selectList(new LambdaQueryWrapper<StoryLocation>()
                .eq(StoryLocation::getProjectId, projectId)
                .orderByAsc(StoryLocation::getId));
        return selectByReference(all, MAX_LOCATIONS, item ->
                contains(referenceText, item.getName())
                        || contains(referenceText, item.getDescription())
                        || contains(referenceText, item.getKeyFeatures()));
    }

    private List<StoryItem> selectItems(Long projectId, String referenceText) {
        List<StoryItem> all = storyItemMapper.selectList(new LambdaQueryWrapper<StoryItem>()
                .eq(StoryItem::getProjectId, projectId)
                .orderByAsc(StoryItem::getId));
        return selectByReference(all, MAX_ITEMS, item ->
                contains(referenceText, item.getName())
                        || contains(referenceText, item.getDescription())
                        || contains(referenceText, item.getUsageRules()));
    }

    private List<EntityRelation> selectRelations(
            Long projectId,
            List<StoryCharacter> characters,
            List<Organization> organizations,
            List<StoryLocation> locations,
            List<StoryItem> items) {
        Set<Long> characterIds = characters.stream().map(StoryCharacter::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> organizationIds = organizations.stream().map(Organization::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> locationIds = locations.stream().map(StoryLocation::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> itemIds = items.stream().map(StoryItem::getId).collect(Collectors.toCollection(LinkedHashSet::new));

        if (characterIds.isEmpty() && organizationIds.isEmpty() && locationIds.isEmpty() && itemIds.isEmpty()) {
            return List.of();
        }
        return entityRelationMapper.selectList(new LambdaQueryWrapper<EntityRelation>()
                .eq(EntityRelation::getProjectId, projectId)
                .eq(EntityRelation::getRelationStatus, "active")
                .and(wrapper -> appendRelationEntityFilters(
                        wrapper,
                        characterIds,
                        organizationIds,
                        locationIds,
                        itemIds))
                .orderByAsc(EntityRelation::getId)
                .last("LIMIT " + MAX_RELATIONS));
    }

    private List<EntityStateRecord> selectStateRecords(
            Long projectId,
            List<StoryCharacter> characters,
            List<Organization> organizations,
            List<StoryLocation> locations,
            List<StoryItem> items) {
        Set<Long> characterIds = characters.stream().map(StoryCharacter::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> organizationIds = organizations.stream().map(Organization::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> locationIds = locations.stream().map(StoryLocation::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> itemIds = items.stream().map(StoryItem::getId).collect(Collectors.toCollection(LinkedHashSet::new));

        if (characterIds.isEmpty() && organizationIds.isEmpty() && locationIds.isEmpty() && itemIds.isEmpty()) {
            return List.of();
        }
        return entityStateRecordMapper.selectList(new LambdaQueryWrapper<EntityStateRecord>()
                .eq(EntityStateRecord::getProjectId, projectId)
                .and(wrapper -> appendStateRecordEntityFilters(
                        wrapper,
                        characterIds,
                        organizationIds,
                        locationIds,
                        itemIds))
                .orderByDesc(EntityStateRecord::getId)
                .last("LIMIT " + MAX_STATE_RECORDS));
    }

    private <T> List<T> selectByReference(List<T> all, int limit, Predicate<T> matcher) {
        List<T> matched = all.stream().filter(matcher).limit(limit).toList();
        if (!matched.isEmpty()) {
            return matched;
        }
        return all.stream().limit(limit).toList();
    }

    private boolean matchesCharacter(StoryCharacter character, String referenceText) {
        if (contains(referenceText, character.getName())
                || contains(referenceText, character.getIdentity())
                || contains(referenceText, character.getPublicIdentity())) {
            return true;
        }
        if (character.getAlias() == null || character.getAlias().isEmpty()) {
            return false;
        }
        return character.getAlias().stream().anyMatch(alias -> contains(referenceText, alias));
    }

    private void appendRelationEntityFilters(
            LambdaQueryWrapper<EntityRelation> wrapper,
            Set<Long> characterIds,
            Set<Long> organizationIds,
            Set<Long> locationIds,
            Set<Long> itemIds) {
        boolean appended = false;
        appended = appendRelationEntityFilter(wrapper, "character", characterIds, appended);
        appended = appendRelationEntityFilter(wrapper, "organization", organizationIds, appended);
        appended = appendRelationEntityFilter(wrapper, "location", locationIds, appended);
        appendRelationEntityFilter(wrapper, "item", itemIds, appended);
    }

    private boolean appendRelationEntityFilter(
            LambdaQueryWrapper<EntityRelation> wrapper,
            String entityType,
            Set<Long> entityIds,
            boolean appended) {
        if (entityIds.isEmpty()) {
            return appended;
        }
        if (!appended) {
            wrapper.nested(item -> relationEntityCondition(item, entityType, entityIds));
            return true;
        }
        wrapper.or(item -> item
                .nested(nested -> relationEntityCondition(nested, entityType, entityIds)));
        return true;
    }

    private void relationEntityCondition(
            LambdaQueryWrapper<EntityRelation> wrapper,
            String entityType,
            Set<Long> entityIds) {
        wrapper.eq(EntityRelation::getSourceType, entityType)
                .in(EntityRelation::getSourceId, entityIds)
                .or()
                .eq(EntityRelation::getTargetType, entityType)
                .in(EntityRelation::getTargetId, entityIds);
    }

    private void appendStateRecordEntityFilters(
            LambdaQueryWrapper<EntityStateRecord> wrapper,
            Set<Long> characterIds,
            Set<Long> organizationIds,
            Set<Long> locationIds,
            Set<Long> itemIds) {
        boolean appended = false;
        appended = appendStateRecordEntityFilter(wrapper, "character", characterIds, appended);
        appended = appendStateRecordEntityFilter(wrapper, "organization", organizationIds, appended);
        appended = appendStateRecordEntityFilter(wrapper, "location", locationIds, appended);
        appendStateRecordEntityFilter(wrapper, "item", itemIds, appended);
    }

    private boolean appendStateRecordEntityFilter(
            LambdaQueryWrapper<EntityStateRecord> wrapper,
            String entityType,
            Set<Long> entityIds,
            boolean appended) {
        if (entityIds.isEmpty()) {
            return appended;
        }
        if (!appended) {
            wrapper.nested(item -> stateRecordEntityCondition(item, entityType, entityIds));
            return true;
        }
        wrapper.or(item -> item
                .nested(nested -> stateRecordEntityCondition(nested, entityType, entityIds)));
        return true;
    }

    private void stateRecordEntityCondition(
            LambdaQueryWrapper<EntityStateRecord> wrapper,
            String entityType,
            Set<Long> entityIds) {
        wrapper.eq(EntityStateRecord::getEntityType, entityType)
                .in(EntityStateRecord::getEntityId, entityIds);
    }

    private String characterText(StoryCharacter character) {
        return "name=%s | role=%s | identity=%s | goal=%s | status=%s".formatted(
                blankToEmpty(character.getName()),
                blankToEmpty(character.getNarrativeRole()),
                blankToEmpty(character.getIdentity()),
                blankToEmpty(character.getCoreGoal()),
                blankToEmpty(character.getStatus()));
    }

    private String organizationText(Organization organization) {
        return "name=%s | type=%s | publicMission=%s | realGoal=%s".formatted(
                blankToEmpty(organization.getName()),
                blankToEmpty(organization.getOrganizationType()),
                blankToEmpty(organization.getPublicMission()),
                blankToEmpty(organization.getRealGoal()));
    }

    private String locationText(StoryLocation location) {
        return "name=%s | type=%s | features=%s | risk=%s".formatted(
                blankToEmpty(location.getName()),
                blankToEmpty(location.getLocationType()),
                blankToEmpty(location.getKeyFeatures()),
                blankToEmpty(location.getRiskLevel()));
    }

    private String itemText(StoryItem item, Map<Long, String> characterNames, Map<Long, String> organizationNames) {
        String owner = "";
        if (item.getOwnerCharacterId() != null) {
            owner = blankToEmpty(characterNames.get(item.getOwnerCharacterId()));
        } else if (item.getOwnerOrgId() != null) {
            owner = blankToEmpty(organizationNames.get(item.getOwnerOrgId()));
        }
        return "name=%s | type=%s | status=%s | owner=%s | limitations=%s".formatted(
                blankToEmpty(item.getName()),
                blankToEmpty(item.getItemType()),
                blankToEmpty(item.getStatus()),
                blankToEmpty(owner),
                blankToEmpty(item.getLimitations()));
    }

    private String relationText(
            EntityRelation relation,
            Map<Long, String> characterNames,
            Map<Long, String> organizationNames,
            Map<Long, String> locationNames,
            Map<Long, String> itemNames) {
        return "source=%s | type=%s | target=%s".formatted(
                entityName(relation.getSourceType(), relation.getSourceId(), characterNames, organizationNames, locationNames, itemNames),
                blankToEmpty(relation.getRelationType()),
                entityName(relation.getTargetType(), relation.getTargetId(), characterNames, organizationNames, locationNames, itemNames));
    }

    private String stateRecordText(
            EntityStateRecord record,
            Map<Long, String> characterNames,
            Map<Long, String> organizationNames,
            Map<Long, String> locationNames,
            Map<Long, String> itemNames) {
        return "entity=%s | stateType=%s | newValue=%s".formatted(
                entityName(record.getEntityType(), record.getEntityId(), characterNames, organizationNames, locationNames, itemNames),
                blankToEmpty(record.getStateType()),
                summarizeMap(record.getNewValue()));
    }

    private String entityName(
            String entityType,
            Long entityId,
            Map<Long, String> characterNames,
            Map<Long, String> organizationNames,
            Map<Long, String> locationNames,
            Map<Long, String> itemNames) {
        if (entityId == null) {
            return blankToEmpty(entityType) + "#?";
        }
        String value = switch (blankToEmpty(entityType)) {
            case "character" -> characterNames.get(entityId);
            case "organization" -> organizationNames.get(entityId);
            case "location" -> locationNames.get(entityId);
            case "item" -> itemNames.get(entityId);
            default -> null;
        };
        return value == null || value.isBlank() ? blankToEmpty(entityType) + "#" + entityId : value;
    }

    private String summarizeMap(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return "none";
        }
        List<String> parts = new ArrayList<>();
        value.forEach((key, item) -> parts.add(key + "=" + blankToEmpty(String.valueOf(item))));
        return String.join(", ", parts);
    }

    private String normalizeReference(String title, String outline, List<String> scenePlan, String previousSummary) {
        StringBuilder builder = new StringBuilder();
        builder.append(blankToEmpty(title)).append('\n');
        builder.append(blankToEmpty(outline)).append('\n');
        builder.append(blankToEmpty(previousSummary)).append('\n');
        if (scenePlan != null) {
            scenePlan.forEach(item -> builder.append(blankToEmpty(item)).append('\n'));
        }
        return builder.toString().toLowerCase(Locale.ROOT);
    }

    private boolean contains(String referenceText, String value) {
        String normalizedValue = blankToEmpty(value).toLowerCase(Locale.ROOT);
        return !normalizedValue.isBlank() && referenceText.contains(normalizedValue);
    }

    private String blankToEmpty(String value) {
        return value == null ? "" : value;
    }
}
