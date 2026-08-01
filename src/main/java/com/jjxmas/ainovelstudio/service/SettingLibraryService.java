package com.jjxmas.ainovelstudio.service;

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
import java.util.List;

public interface SettingLibraryService {

    SettingLibraryResponse generateSettingLibrary(SettingLibraryGenerateRequest request);

    SettingLibraryResponse getSettingLibrary(Long projectId);

    SettingLibraryResponse updateSettingLibrary(Long projectId, SettingLibraryUpdateRequest request);

    SettingLibraryResponse updateSettingLibraryById(Long settingLibraryId, SettingLibraryUpdateRequest request);

    SettingLibraryResponse rewriteSettingLibrary(Long projectId, SettingLibraryRewriteRequest request);

    SettingLibraryResponse confirmSettingLibrary(Long projectId);

    SettingLibraryResponse confirmSettingLibraryById(Long settingLibraryId);

    List<StoryCharacterResponse> listCharacters(Long projectId);

    StoryCharacterResponse createCharacter(Long projectId, StoryCharacterUpsertRequest request);

    StoryCharacterResponse updateCharacter(Long projectId, Long characterId, StoryCharacterUpsertRequest request);

    void deleteCharacter(Long projectId, Long characterId);

    List<OrganizationResponse> listOrganizations(Long projectId);

    OrganizationResponse createOrganization(Long projectId, OrganizationUpsertRequest request);

    OrganizationResponse updateOrganization(Long projectId, Long organizationId, OrganizationUpsertRequest request);

    void deleteOrganization(Long projectId, Long organizationId);

    List<StoryLocationResponse> listLocations(Long projectId);

    StoryLocationResponse createLocation(Long projectId, StoryLocationUpsertRequest request);

    StoryLocationResponse updateLocation(Long projectId, Long locationId, StoryLocationUpsertRequest request);

    void deleteLocation(Long projectId, Long locationId);

    List<StoryItemResponse> listItems(Long projectId);

    StoryItemResponse createItem(Long projectId, StoryItemUpsertRequest request);

    StoryItemResponse updateItem(Long projectId, Long itemId, StoryItemUpsertRequest request);

    void deleteItem(Long projectId, Long itemId);

    List<WorldRuleResponse> listWorldRules(Long projectId);

    WorldRuleResponse createWorldRule(Long projectId, WorldRuleUpsertRequest request);

    WorldRuleResponse updateWorldRule(Long projectId, Long ruleId, WorldRuleUpsertRequest request);

    void deleteWorldRule(Long projectId, Long ruleId);

    List<EntityRelationResponse> listRelations(Long projectId);

    EntityRelationResponse createRelation(Long projectId, EntityRelationUpsertRequest request);

    EntityRelationResponse updateRelation(Long projectId, Long relationId, EntityRelationUpsertRequest request);

    void deleteRelation(Long projectId, Long relationId);

    List<StoryEventResponse> listEvents(Long projectId);

    StoryEventResponse createEvent(Long projectId, StoryEventUpsertRequest request);

    StoryEventResponse updateEvent(Long projectId, Long eventId, StoryEventUpsertRequest request);

    void deleteEvent(Long projectId, Long eventId);

    List<EntityStateRecordResponse> listStateRecords(Long projectId);

    EntityStateRecordResponse createStateRecord(Long projectId, EntityStateRecordUpsertRequest request);

    EntityStateRecordResponse updateStateRecord(Long projectId, Long recordId, EntityStateRecordUpsertRequest request);

    void deleteStateRecord(Long projectId, Long recordId);
}
