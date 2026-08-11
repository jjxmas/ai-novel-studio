package com.jjxmas.ainovelstudio.converter;

import com.jjxmas.ainovelstudio.pojo.dto.EntityRelationResponse;
import com.jjxmas.ainovelstudio.pojo.dto.EntityRelationUpsertRequest;
import com.jjxmas.ainovelstudio.pojo.dto.EntityStateRecordResponse;
import com.jjxmas.ainovelstudio.pojo.dto.EntityStateRecordUpsertRequest;
import com.jjxmas.ainovelstudio.pojo.dto.OrganizationResponse;
import com.jjxmas.ainovelstudio.pojo.dto.OrganizationUpsertRequest;
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
import com.jjxmas.ainovelstudio.pojo.entity.Organization;
import com.jjxmas.ainovelstudio.pojo.entity.StoryCharacter;
import com.jjxmas.ainovelstudio.pojo.entity.StoryEvent;
import com.jjxmas.ainovelstudio.pojo.entity.StoryItem;
import com.jjxmas.ainovelstudio.pojo.entity.StoryLocation;
import com.jjxmas.ainovelstudio.pojo.entity.WorldRule;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SettingLibraryConverter {

    @Mapping(target = "aliases", source = "alias")
    StoryCharacterResponse toCharacterResponse(StoryCharacter character);

    List<StoryCharacterResponse> toCharacterResponseList(List<StoryCharacter> characters);

    @Mapping(target = "alias", source = "aliases")
    StoryCharacter toCharacter(StoryCharacterUpsertRequest request);

    @Mapping(target = "alias", source = "aliases")
    void updateCharacter(StoryCharacterUpsertRequest request, @MappingTarget StoryCharacter character);

    OrganizationResponse toOrganizationResponse(Organization organization);

    List<OrganizationResponse> toOrganizationResponseList(List<Organization> organizations);

    Organization toOrganization(OrganizationUpsertRequest request);

    void updateOrganization(OrganizationUpsertRequest request, @MappingTarget Organization organization);

    StoryLocationResponse toLocationResponse(StoryLocation location);

    List<StoryLocationResponse> toLocationResponseList(List<StoryLocation> locations);

    StoryLocation toLocation(StoryLocationUpsertRequest request);

    void updateLocation(StoryLocationUpsertRequest request, @MappingTarget StoryLocation location);

    StoryItemResponse toItemResponse(StoryItem item);

    List<StoryItemResponse> toItemResponseList(List<StoryItem> items);

    StoryItem toItem(StoryItemUpsertRequest request);

    void updateItem(StoryItemUpsertRequest request, @MappingTarget StoryItem item);

    WorldRuleResponse toWorldRuleResponse(WorldRule worldRule);

    List<WorldRuleResponse> toWorldRuleResponseList(List<WorldRule> worldRules);

    WorldRule toWorldRule(WorldRuleUpsertRequest request);

    void updateWorldRule(WorldRuleUpsertRequest request, @MappingTarget WorldRule worldRule);

    EntityRelationResponse toRelationResponse(EntityRelation relation);

    List<EntityRelationResponse> toRelationResponseList(List<EntityRelation> relations);

    EntityRelation toRelation(EntityRelationUpsertRequest request);

    void updateRelation(EntityRelationUpsertRequest request, @MappingTarget EntityRelation relation);

    @Mapping(target = "planned", source = "isPlanned")
    StoryEventResponse toEventResponse(StoryEvent event);

    List<StoryEventResponse> toEventResponseList(List<StoryEvent> events);

    @Mapping(target = "isPlanned", source = "planned")
    StoryEvent toEvent(StoryEventUpsertRequest request);

    @Mapping(target = "isPlanned", source = "planned")
    void updateEvent(StoryEventUpsertRequest request, @MappingTarget StoryEvent event);

    EntityStateRecordResponse toStateRecordResponse(EntityStateRecord record);

    List<EntityStateRecordResponse> toStateRecordResponseList(List<EntityStateRecord> records);

    EntityStateRecord toStateRecord(EntityStateRecordUpsertRequest request);

    void updateStateRecord(EntityStateRecordUpsertRequest request, @MappingTarget EntityStateRecord record);
}
