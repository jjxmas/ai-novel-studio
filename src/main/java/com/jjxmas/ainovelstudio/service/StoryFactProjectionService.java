package com.jjxmas.ainovelstudio.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jjxmas.ainovelstudio.mapper.EntityRelationMapper;
import com.jjxmas.ainovelstudio.mapper.EntityStateRecordMapper;
import com.jjxmas.ainovelstudio.mapper.OrganizationMapper;
import com.jjxmas.ainovelstudio.mapper.StoryCharacterMapper;
import com.jjxmas.ainovelstudio.mapper.StoryEventMapper;
import com.jjxmas.ainovelstudio.mapper.StoryItemMapper;
import com.jjxmas.ainovelstudio.mapper.StoryLocationMapper;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterFactExtraction;
import com.jjxmas.ainovelstudio.pojo.entity.Chapter;
import com.jjxmas.ainovelstudio.pojo.entity.EntityRelation;
import com.jjxmas.ainovelstudio.pojo.entity.EntityStateRecord;
import com.jjxmas.ainovelstudio.pojo.entity.Organization;
import com.jjxmas.ainovelstudio.pojo.entity.StoryCharacter;
import com.jjxmas.ainovelstudio.pojo.entity.StoryEvent;
import com.jjxmas.ainovelstudio.pojo.entity.StoryItem;
import com.jjxmas.ainovelstudio.pojo.entity.StoryLocation;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StoryFactProjectionService {

    private final StoryEventMapper storyEventMapper;
    private final EntityStateRecordMapper entityStateRecordMapper;
    private final EntityRelationMapper entityRelationMapper;
    private final StoryCharacterMapper storyCharacterMapper;
    private final OrganizationMapper organizationMapper;
    private final StoryLocationMapper storyLocationMapper;
    private final StoryItemMapper storyItemMapper;

    public StoryFactProjectionService(
            StoryEventMapper storyEventMapper,
            EntityStateRecordMapper entityStateRecordMapper,
            EntityRelationMapper entityRelationMapper,
            StoryCharacterMapper storyCharacterMapper,
            OrganizationMapper organizationMapper,
            StoryLocationMapper storyLocationMapper,
            StoryItemMapper storyItemMapper) {
        this.storyEventMapper = storyEventMapper;
        this.entityStateRecordMapper = entityStateRecordMapper;
        this.entityRelationMapper = entityRelationMapper;
        this.storyCharacterMapper = storyCharacterMapper;
        this.organizationMapper = organizationMapper;
        this.storyLocationMapper = storyLocationMapper;
        this.storyItemMapper = storyItemMapper;
    }

    @Transactional
    public void projectChapterFacts(Chapter chapter, ChapterFactExtraction extraction) {
        if (chapter == null || extraction == null) {
            return;
        }

        EntityLookup lookup = loadLookup(chapter.getProjectId());
        cleanupExistingChapterProjection(chapter);
        List<StoryEvent> createdEvents = projectEvents(chapter, extraction, lookup);
        Long primaryEventId = createdEvents.isEmpty() ? null : createdEvents.get(0).getId();
        projectStateChanges(chapter, extraction, lookup, primaryEventId);
        projectRelationChanges(chapter, extraction, lookup, primaryEventId);
    }

    private void cleanupExistingChapterProjection(Chapter chapter) {
        List<Long> chapterEventIds = storyEventMapper.selectList(new LambdaQueryWrapper<StoryEvent>()
                        .eq(StoryEvent::getProjectId, chapter.getProjectId())
                        .eq(StoryEvent::getChapterId, chapter.getId())
                        .eq(StoryEvent::getIsPlanned, false))
                .stream()
                .map(StoryEvent::getId)
                .toList();

        entityStateRecordMapper.delete(new LambdaQueryWrapper<EntityStateRecord>()
                .eq(EntityStateRecord::getProjectId, chapter.getProjectId())
                .eq(EntityStateRecord::getChapterId, chapter.getId()));

        if (!chapterEventIds.isEmpty()) {
            entityRelationMapper.delete(new LambdaQueryWrapper<EntityRelation>()
                    .eq(EntityRelation::getProjectId, chapter.getProjectId())
                    .and(query -> query.in(EntityRelation::getStartEventId, chapterEventIds)
                            .or()
                            .in(EntityRelation::getEndEventId, chapterEventIds)));

            storyEventMapper.delete(new LambdaQueryWrapper<StoryEvent>()
                    .eq(StoryEvent::getProjectId, chapter.getProjectId())
                    .eq(StoryEvent::getChapterId, chapter.getId())
                    .eq(StoryEvent::getIsPlanned, false));
        }
    }

    private List<StoryEvent> projectEvents(Chapter chapter, ChapterFactExtraction extraction, EntityLookup lookup) {
        List<StoryEvent> created = new ArrayList<>();
        for (ChapterFactExtraction.EventFact item : defaultList(extraction.getEvents())) {
            if (item == null || isBlank(item.getName())) {
                continue;
            }
            StoryEvent event = new StoryEvent()
                    .setProjectId(chapter.getProjectId())
                    .setName(item.getName())
                    .setEventType(blankToDefault(item.getEventType(), "story"))
                    .setDescription(blankToEmpty(item.getDescription()))
                    .setEventTimeText(blankToEmpty(item.getEventTimeText()))
                    .setLocationId(resolveLocationId(lookup, item.getLocationText()))
                    .setChapterId(chapter.getId())
                    .setIsPlanned(false)
                    .setImportance(item.getImportance() == null ? 0 : item.getImportance());
            storyEventMapper.insert(event);
            created.add(event);
        }
        return created;
    }

    private void projectStateChanges(
            Chapter chapter,
            ChapterFactExtraction extraction,
            EntityLookup lookup,
            Long primaryEventId) {
        for (ChapterFactExtraction.StateChangeFact item : defaultList(extraction.getStateChanges())) {
            if (item == null) {
                continue;
            }
            EntityRef entityRef = resolveEntity(lookup, item.getEntityType(), item.getEntityName());
            if (entityRef == null || entityRef.entityId() == null || isBlank(item.getStateType())) {
                continue;
            }
            EntityStateRecord record = new EntityStateRecord()
                    .setProjectId(chapter.getProjectId())
                    .setEntityType(entityRef.entityType())
                    .setEntityId(entityRef.entityId())
                    .setStateType(item.getStateType())
                    .setOldValue(item.getOldValue() == null ? Map.of() : item.getOldValue())
                    .setNewValue(item.getNewValue() == null ? Map.of() : item.getNewValue())
                    .setEventId(primaryEventId)
                    .setChapterId(chapter.getId())
                    .setEffectiveAt(LocalDateTime.now());
            entityStateRecordMapper.insert(record);
        }
    }

    private void projectRelationChanges(
            Chapter chapter,
            ChapterFactExtraction extraction,
            EntityLookup lookup,
            Long primaryEventId) {
        for (ChapterFactExtraction.RelationChangeFact item : defaultList(extraction.getRelationChanges())) {
            if (item == null || isBlank(item.getRelationType())) {
                continue;
            }
            EntityRef source = resolveEntity(lookup, item.getSourceType(), item.getSourceName());
            EntityRef target = resolveEntity(lookup, item.getTargetType(), item.getTargetName());
            if (source == null || target == null) {
                continue;
            }

            EntityRelation existing = entityRelationMapper.selectOne(new LambdaQueryWrapper<EntityRelation>()
                    .eq(EntityRelation::getProjectId, chapter.getProjectId())
                    .eq(EntityRelation::getSourceType, source.entityType())
                    .eq(EntityRelation::getSourceId, source.entityId())
                    .eq(EntityRelation::getTargetType, target.entityType())
                    .eq(EntityRelation::getTargetId, target.entityId())
                    .eq(EntityRelation::getRelationType, item.getRelationType())
                    .orderByDesc(EntityRelation::getId)
                    .last("LIMIT 1"));

            String changeType = normalizeKey(item.getChangeType());
            if (isRelationEnd(changeType)) {
                if (existing != null && "active".equalsIgnoreCase(blankToDefault(existing.getRelationStatus(), "active"))) {
                    existing.setRelationStatus("ended")
                            .setEndEventId(primaryEventId)
                            .setNote(blankToDefault(item.getNote(), existing.getNote()));
                    entityRelationMapper.updateById(existing);
                }
                continue;
            }

            if (existing == null) {
                EntityRelation relation = new EntityRelation()
                        .setProjectId(chapter.getProjectId())
                        .setSourceType(source.entityType())
                        .setSourceId(source.entityId())
                        .setTargetType(target.entityType())
                        .setTargetId(target.entityId())
                        .setRelationType(item.getRelationType())
                        .setRelationStatus("active")
                        .setVisibilityLevel("public")
                        .setNote(blankToEmpty(item.getNote()))
                        .setStartEventId(primaryEventId);
                entityRelationMapper.insert(relation);
                continue;
            }

            boolean wasEnded = "ended".equalsIgnoreCase(blankToDefault(existing.getRelationStatus(), ""));
            existing.setRelationStatus("active")
                    .setNote(blankToDefault(item.getNote(), existing.getNote()));
            if (existing.getStartEventId() == null) {
                existing.setStartEventId(primaryEventId);
            }
            if (wasEnded) {
                existing.setEndEventId(null);
            }
            entityRelationMapper.updateById(existing);
        }
    }

    private EntityLookup loadLookup(Long projectId) {
        Map<String, Long> characterNames = new LinkedHashMap<>();
        for (StoryCharacter item : storyCharacterMapper.selectList(new LambdaQueryWrapper<StoryCharacter>()
                .eq(StoryCharacter::getProjectId, projectId))) {
            putIfPresent(characterNames, item.getName(), item.getId());
            if (item.getAlias() != null) {
                item.getAlias().forEach(alias -> putIfPresent(characterNames, alias, item.getId()));
            }
        }

        Map<String, Long> organizationNames = loadNameMap(
                organizationMapper.selectList(new LambdaQueryWrapper<Organization>()
                        .eq(Organization::getProjectId, projectId)),
                Organization::getName,
                Organization::getId);
        Map<String, Long> locationNames = loadNameMap(
                storyLocationMapper.selectList(new LambdaQueryWrapper<StoryLocation>()
                        .eq(StoryLocation::getProjectId, projectId)),
                StoryLocation::getName,
                StoryLocation::getId);
        Map<String, Long> itemNames = loadNameMap(
                storyItemMapper.selectList(new LambdaQueryWrapper<StoryItem>()
                        .eq(StoryItem::getProjectId, projectId)),
                StoryItem::getName,
                StoryItem::getId);
        return new EntityLookup(characterNames, organizationNames, locationNames, itemNames);
    }

    private <T> Map<String, Long> loadNameMap(List<T> items, java.util.function.Function<T, String> nameGetter, java.util.function.Function<T, Long> idGetter) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (T item : items) {
            putIfPresent(result, nameGetter.apply(item), idGetter.apply(item));
        }
        return result;
    }

    private void putIfPresent(Map<String, Long> target, String key, Long value) {
        String normalized = normalizeKey(key);
        if (!normalized.isBlank() && value != null) {
            target.putIfAbsent(normalized, value);
        }
    }

    private Long resolveLocationId(EntityLookup lookup, String locationText) {
        String normalized = normalizeKey(locationText);
        return normalized.isBlank() ? null : lookup.locationNames().get(normalized);
    }

    private EntityRef resolveEntity(EntityLookup lookup, String entityType, String entityName) {
        String normalizedType = normalizeKey(entityType);
        String normalizedName = normalizeKey(entityName);
        if (normalizedType.isBlank() || normalizedName.isBlank()) {
            return null;
        }
        return switch (normalizedType) {
            case "character" -> refIfFound("character", lookup.characterNames().get(normalizedName));
            case "organization" -> refIfFound("organization", lookup.organizationNames().get(normalizedName));
            case "location" -> refIfFound("location", lookup.locationNames().get(normalizedName));
            case "item" -> refIfFound("item", lookup.itemNames().get(normalizedName));
            default -> null;
        };
    }

    private EntityRef refIfFound(String entityType, Long entityId) {
        return entityId == null ? null : new EntityRef(entityType, entityId);
    }

    private boolean isRelationEnd(String changeType) {
        return "end".equals(changeType) || "ended".equals(changeType) || "resolved".equals(changeType) || "payoff".equals(changeType);
    }

    private <T> List<T> defaultList(List<T> value) {
        return value == null ? List.of() : value;
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String blankToEmpty(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalizeKey(String value) {
        return blankToEmpty(value).trim().toLowerCase(Locale.ROOT);
    }

    private record EntityRef(String entityType, Long entityId) {
    }

    private record EntityLookup(
            Map<String, Long> characterNames,
            Map<String, Long> organizationNames,
            Map<String, Long> locationNames,
            Map<String, Long> itemNames) {
    }
}
