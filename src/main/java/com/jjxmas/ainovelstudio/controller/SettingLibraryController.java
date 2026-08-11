package com.jjxmas.ainovelstudio.controller;

import com.jjxmas.ainovelstudio.common.api.ApiResponse;
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
import com.jjxmas.ainovelstudio.pojo.dto.SettingWorkflowCreateRequest;
import com.jjxmas.ainovelstudio.pojo.dto.SettingWorkflowRegenerateModuleRequest;
import com.jjxmas.ainovelstudio.pojo.dto.SettingWorkflowResponse;
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
import com.jjxmas.ainovelstudio.service.SettingLibraryService;
import com.jjxmas.ainovelstudio.service.SettingWorkflowService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class SettingLibraryController {

    private final SettingLibraryService settingLibraryService;
    private final SettingWorkflowService settingWorkflowService;

    @GetMapping("/setting-library/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.success("setting-library-ready");
    }

    @PostMapping("/projects/{projectId}/setting-library/generate")
    public ApiResponse<SettingLibraryResponse> generateSettingLibrary(
            @PathVariable Long projectId,
            @Valid @RequestBody SettingLibraryGenerateRequest request) {
        request.setProjectId(projectId);
        return ApiResponse.success("setting-library-generated", settingLibraryService.generateSettingLibrary(request));
    }

    @PostMapping("/projects/{projectId}/setting-workflows")
    public ApiResponse<SettingWorkflowResponse> startSettingWorkflow(
            @PathVariable Long projectId,
            @Valid @RequestBody SettingWorkflowCreateRequest request) {
        request.setProjectId(projectId);
        return ApiResponse.success("setting-workflow-started", settingWorkflowService.startWorkflow(request));
    }

    @GetMapping("/projects/{projectId}/setting-workflows/latest")
    public ApiResponse<SettingWorkflowResponse> getLatestSettingWorkflow(@PathVariable Long projectId) {
        return ApiResponse.success(settingWorkflowService.getLatestWorkflow(projectId));
    }

    @GetMapping("/setting-workflows/{workflowId}")
    public ApiResponse<SettingWorkflowResponse> getSettingWorkflow(@PathVariable Long workflowId) {
        return ApiResponse.success(settingWorkflowService.getWorkflow(workflowId));
    }

    @PostMapping("/setting-workflows/{workflowId}/approve-blueprint")
    public ApiResponse<SettingWorkflowResponse> approveSettingWorkflowBlueprint(@PathVariable Long workflowId) {
        return ApiResponse.success("setting-workflow-draft-ready", settingWorkflowService.approveBlueprint(workflowId));
    }

    @PostMapping("/setting-workflows/{workflowId}/regenerate-module")
    public ApiResponse<SettingWorkflowResponse> regenerateSettingWorkflowModule(
            @PathVariable Long workflowId,
            @Valid @RequestBody SettingWorkflowRegenerateModuleRequest request) {
        return ApiResponse.success("setting-workflow-module-regenerated",
                settingWorkflowService.regenerateModule(workflowId, request.getModuleKey()));
    }

    @PostMapping("/setting-workflows/{workflowId}/commit")
    public ApiResponse<SettingLibraryResponse> commitSettingWorkflow(@PathVariable Long workflowId) {
        return ApiResponse.success("setting-workflow-committed", settingWorkflowService.commitWorkflow(workflowId));
    }

    @GetMapping("/projects/{projectId}/setting-library")
    public ApiResponse<SettingLibraryResponse> getSettingLibrary(@PathVariable Long projectId) {
        return ApiResponse.success(settingLibraryService.getSettingLibrary(projectId));
    }

    @PatchMapping("/projects/{projectId}/setting-library")
    public ApiResponse<SettingLibraryResponse> updateSettingLibrary(
            @PathVariable Long projectId,
            @Valid @RequestBody SettingLibraryUpdateRequest request) {
        return ApiResponse.success("setting-library-saved", settingLibraryService.updateSettingLibrary(projectId, request));
    }

    @PatchMapping("/setting-library/{settingLibraryId}")
    public ApiResponse<SettingLibraryResponse> updateSettingLibraryById(
            @PathVariable Long settingLibraryId,
            @Valid @RequestBody SettingLibraryUpdateRequest request) {
        return ApiResponse.success("setting-library-saved", settingLibraryService.updateSettingLibraryById(settingLibraryId, request));
    }

    @PostMapping("/projects/{projectId}/setting-library/regenerate")
    public ApiResponse<SettingLibraryResponse> rewriteSettingLibrary(
            @PathVariable Long projectId,
            @Valid @RequestBody SettingLibraryRewriteRequest request) {
        return ApiResponse.success("setting-library-regenerated", settingLibraryService.rewriteSettingLibrary(projectId, request));
    }

    @PostMapping("/projects/{projectId}/setting-library/confirm")
    public ApiResponse<SettingLibraryResponse> confirmSettingLibrary(@PathVariable Long projectId) {
        return ApiResponse.success("setting-library-confirmed", settingLibraryService.confirmSettingLibrary(projectId));
    }

    @PostMapping("/setting-library/{settingLibraryId}/confirm")
    public ApiResponse<SettingLibraryResponse> confirmSettingLibraryById(@PathVariable Long settingLibraryId) {
        return ApiResponse.success("setting-library-confirmed", settingLibraryService.confirmSettingLibraryById(settingLibraryId));
    }

    @GetMapping("/projects/{projectId}/characters")
    public ApiResponse<List<StoryCharacterResponse>> listCharacters(@PathVariable Long projectId) {
        return ApiResponse.success(settingLibraryService.listCharacters(projectId));
    }

    @PostMapping("/projects/{projectId}/characters")
    public ApiResponse<Long> createCharacter(
            @PathVariable Long projectId,
            @Valid @RequestBody StoryCharacterUpsertRequest request) {
        return ApiResponse.success("character-created", settingLibraryService.createCharacter(projectId, request));
    }

    @PatchMapping("/projects/{projectId}/characters/{characterId}")
    public ApiResponse<Void> updateCharacter(
            @PathVariable Long projectId,
            @PathVariable Long characterId,
            @Valid @RequestBody StoryCharacterUpsertRequest request) {
        settingLibraryService.updateCharacter(projectId, characterId, request);
        return ApiResponse.success("character-saved", null);
    }

    @DeleteMapping("/projects/{projectId}/characters/{characterId}")
    public ApiResponse<Void> deleteCharacter(@PathVariable Long projectId, @PathVariable Long characterId) {
        settingLibraryService.deleteCharacter(projectId, characterId);
        return ApiResponse.success("character-deleted", null);
    }

    @GetMapping("/projects/{projectId}/organizations")
    public ApiResponse<List<OrganizationResponse>> listOrganizations(@PathVariable Long projectId) {
        return ApiResponse.success(settingLibraryService.listOrganizations(projectId));
    }

    @PostMapping("/projects/{projectId}/organizations")
    public ApiResponse<Long> createOrganization(
            @PathVariable Long projectId,
            @Valid @RequestBody OrganizationUpsertRequest request) {
        return ApiResponse.success("organization-created", settingLibraryService.createOrganization(projectId, request));
    }

    @PatchMapping("/projects/{projectId}/organizations/{organizationId}")
    public ApiResponse<Void> updateOrganization(
            @PathVariable Long projectId,
            @PathVariable Long organizationId,
            @Valid @RequestBody OrganizationUpsertRequest request) {
        settingLibraryService.updateOrganization(projectId, organizationId, request);
        return ApiResponse.success("organization-saved", null);
    }

    @DeleteMapping("/projects/{projectId}/organizations/{organizationId}")
    public ApiResponse<Void> deleteOrganization(@PathVariable Long projectId, @PathVariable Long organizationId) {
        settingLibraryService.deleteOrganization(projectId, organizationId);
        return ApiResponse.success("organization-deleted", null);
    }

    @GetMapping("/projects/{projectId}/locations")
    public ApiResponse<List<StoryLocationResponse>> listLocations(@PathVariable Long projectId) {
        return ApiResponse.success(settingLibraryService.listLocations(projectId));
    }

    @PostMapping("/projects/{projectId}/locations")
    public ApiResponse<Long> createLocation(
            @PathVariable Long projectId,
            @Valid @RequestBody StoryLocationUpsertRequest request) {
        return ApiResponse.success("location-created", settingLibraryService.createLocation(projectId, request));
    }

    @PatchMapping("/projects/{projectId}/locations/{locationId}")
    public ApiResponse<Void> updateLocation(
            @PathVariable Long projectId,
            @PathVariable Long locationId,
            @Valid @RequestBody StoryLocationUpsertRequest request) {
        settingLibraryService.updateLocation(projectId, locationId, request);
        return ApiResponse.success("location-saved", null);
    }

    @DeleteMapping("/projects/{projectId}/locations/{locationId}")
    public ApiResponse<Void> deleteLocation(@PathVariable Long projectId, @PathVariable Long locationId) {
        settingLibraryService.deleteLocation(projectId, locationId);
        return ApiResponse.success("location-deleted", null);
    }

    @GetMapping("/projects/{projectId}/items")
    public ApiResponse<List<StoryItemResponse>> listItems(@PathVariable Long projectId) {
        return ApiResponse.success(settingLibraryService.listItems(projectId));
    }

    @PostMapping("/projects/{projectId}/items")
    public ApiResponse<Long> createItem(
            @PathVariable Long projectId,
            @Valid @RequestBody StoryItemUpsertRequest request) {
        return ApiResponse.success("item-created", settingLibraryService.createItem(projectId, request));
    }

    @PatchMapping("/projects/{projectId}/items/{itemId}")
    public ApiResponse<Void> updateItem(
            @PathVariable Long projectId,
            @PathVariable Long itemId,
            @Valid @RequestBody StoryItemUpsertRequest request) {
        settingLibraryService.updateItem(projectId, itemId, request);
        return ApiResponse.success("item-saved", null);
    }

    @DeleteMapping("/projects/{projectId}/items/{itemId}")
    public ApiResponse<Void> deleteItem(@PathVariable Long projectId, @PathVariable Long itemId) {
        settingLibraryService.deleteItem(projectId, itemId);
        return ApiResponse.success("item-deleted", null);
    }

    @GetMapping("/projects/{projectId}/world-rules")
    public ApiResponse<List<WorldRuleResponse>> listWorldRules(@PathVariable Long projectId) {
        return ApiResponse.success(settingLibraryService.listWorldRules(projectId));
    }

    @PostMapping("/projects/{projectId}/world-rules")
    public ApiResponse<Long> createWorldRule(
            @PathVariable Long projectId,
            @Valid @RequestBody WorldRuleUpsertRequest request) {
        return ApiResponse.success("world-rule-created", settingLibraryService.createWorldRule(projectId, request));
    }

    @PatchMapping("/projects/{projectId}/world-rules/{ruleId}")
    public ApiResponse<Void> updateWorldRule(
            @PathVariable Long projectId,
            @PathVariable Long ruleId,
            @Valid @RequestBody WorldRuleUpsertRequest request) {
        settingLibraryService.updateWorldRule(projectId, ruleId, request);
        return ApiResponse.success("world-rule-saved", null);
    }

    @DeleteMapping("/projects/{projectId}/world-rules/{ruleId}")
    public ApiResponse<Void> deleteWorldRule(@PathVariable Long projectId, @PathVariable Long ruleId) {
        settingLibraryService.deleteWorldRule(projectId, ruleId);
        return ApiResponse.success("world-rule-deleted", null);
    }

    @GetMapping("/projects/{projectId}/relations")
    public ApiResponse<List<EntityRelationResponse>> listRelations(@PathVariable Long projectId) {
        return ApiResponse.success(settingLibraryService.listRelations(projectId));
    }

    @PostMapping("/projects/{projectId}/relations")
    public ApiResponse<Long> createRelation(
            @PathVariable Long projectId,
            @Valid @RequestBody EntityRelationUpsertRequest request) {
        return ApiResponse.success("relation-created", settingLibraryService.createRelation(projectId, request));
    }

    @PatchMapping("/projects/{projectId}/relations/{relationId}")
    public ApiResponse<Void> updateRelation(
            @PathVariable Long projectId,
            @PathVariable Long relationId,
            @Valid @RequestBody EntityRelationUpsertRequest request) {
        settingLibraryService.updateRelation(projectId, relationId, request);
        return ApiResponse.success("relation-saved", null);
    }

    @DeleteMapping("/projects/{projectId}/relations/{relationId}")
    public ApiResponse<Void> deleteRelation(@PathVariable Long projectId, @PathVariable Long relationId) {
        settingLibraryService.deleteRelation(projectId, relationId);
        return ApiResponse.success("relation-deleted", null);
    }

    @GetMapping("/projects/{projectId}/events")
    public ApiResponse<List<StoryEventResponse>> listEvents(@PathVariable Long projectId) {
        return ApiResponse.success(settingLibraryService.listEvents(projectId));
    }

    @PostMapping("/projects/{projectId}/events")
    public ApiResponse<Long> createEvent(
            @PathVariable Long projectId,
            @Valid @RequestBody StoryEventUpsertRequest request) {
        return ApiResponse.success("event-created", settingLibraryService.createEvent(projectId, request));
    }

    @PatchMapping("/projects/{projectId}/events/{eventId}")
    public ApiResponse<Void> updateEvent(
            @PathVariable Long projectId,
            @PathVariable Long eventId,
            @Valid @RequestBody StoryEventUpsertRequest request) {
        settingLibraryService.updateEvent(projectId, eventId, request);
        return ApiResponse.success("event-saved", null);
    }

    @DeleteMapping("/projects/{projectId}/events/{eventId}")
    public ApiResponse<Void> deleteEvent(@PathVariable Long projectId, @PathVariable Long eventId) {
        settingLibraryService.deleteEvent(projectId, eventId);
        return ApiResponse.success("event-deleted", null);
    }

    @GetMapping("/projects/{projectId}/state-records")
    public ApiResponse<List<EntityStateRecordResponse>> listStateRecords(@PathVariable Long projectId) {
        return ApiResponse.success(settingLibraryService.listStateRecords(projectId));
    }

    @PostMapping("/projects/{projectId}/state-records")
    public ApiResponse<Long> createStateRecord(
            @PathVariable Long projectId,
            @Valid @RequestBody EntityStateRecordUpsertRequest request) {
        return ApiResponse.success("state-record-created", settingLibraryService.createStateRecord(projectId, request));
    }

    @PatchMapping("/projects/{projectId}/state-records/{recordId}")
    public ApiResponse<Void> updateStateRecord(
            @PathVariable Long projectId,
            @PathVariable Long recordId,
            @Valid @RequestBody EntityStateRecordUpsertRequest request) {
        settingLibraryService.updateStateRecord(projectId, recordId, request);
        return ApiResponse.success("state-record-saved", null);
    }

    @DeleteMapping("/projects/{projectId}/state-records/{recordId}")
    public ApiResponse<Void> deleteStateRecord(@PathVariable Long projectId, @PathVariable Long recordId) {
        settingLibraryService.deleteStateRecord(projectId, recordId);
        return ApiResponse.success("state-record-deleted", null);
    }
}
