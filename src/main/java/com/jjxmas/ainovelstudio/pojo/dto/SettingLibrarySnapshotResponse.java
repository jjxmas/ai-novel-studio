package com.jjxmas.ainovelstudio.pojo.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SettingLibrarySnapshotResponse {

    private SettingLibraryResponse settingLibrary;

    private List<StoryCharacterResponse> characters;

    private List<OrganizationResponse> organizations;

    private List<StoryLocationResponse> locations;

    private List<StoryItemResponse> items;

    private List<WorldRuleResponse> worldRules;

    private List<EntityRelationResponse> relations;

    private List<StoryEventResponse> events;

    private List<EntityStateRecordResponse> stateRecords;
}
