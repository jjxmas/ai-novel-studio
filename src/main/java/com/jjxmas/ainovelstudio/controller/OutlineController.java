package com.jjxmas.ainovelstudio.controller;

import com.jjxmas.ainovelstudio.common.api.ApiResponse;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterResponse;
import com.jjxmas.ainovelstudio.pojo.dto.OutlineGenerateRequest;
import com.jjxmas.ainovelstudio.pojo.dto.OutlineResponse;
import com.jjxmas.ainovelstudio.pojo.dto.OutlineRewriteRequest;
import com.jjxmas.ainovelstudio.pojo.dto.OutlineUpdateRequest;
import com.jjxmas.ainovelstudio.pojo.dto.OutlineWorkflowCreateRequest;
import com.jjxmas.ainovelstudio.pojo.dto.OutlineWorkflowResponse;
import com.jjxmas.ainovelstudio.service.OutlineService;
import com.jjxmas.ainovelstudio.service.OutlineWorkflowService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
public class OutlineController {

    private final OutlineService outlineService;
    private final OutlineWorkflowService outlineWorkflowService;

    @GetMapping("/global-outline/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.success("outline-module-ready");
    }

    @PostMapping("/projects/{projectId}/global-outline/generate")
    public ApiResponse<OutlineResponse> generateGlobalOutline(
            @PathVariable Long projectId,
            @Valid @RequestBody OutlineGenerateRequest request) {
        request.setProjectId(projectId);
        request.setOutlineLevel("global");
        return ApiResponse.success("global-outline-generated", outlineService.generateOutline(request));
    }

    @PostMapping("/projects/{projectId}/outline-workflows")
    public ApiResponse<OutlineWorkflowResponse> startOutlineWorkflow(
            @PathVariable Long projectId,
            @Valid @RequestBody OutlineWorkflowCreateRequest request) {
        request.setProjectId(projectId);
        return ApiResponse.success("outline-workflow-started", outlineWorkflowService.startWorkflow(request));
    }

    @GetMapping("/projects/{projectId}/outline-workflows/latest")
    public ApiResponse<OutlineWorkflowResponse> getLatestOutlineWorkflow(@PathVariable Long projectId) {
        return ApiResponse.success(outlineWorkflowService.getLatestWorkflow(projectId));
    }

    @GetMapping("/outline-workflows/{workflowId}")
    public ApiResponse<OutlineWorkflowResponse> getOutlineWorkflow(@PathVariable Long workflowId) {
        return ApiResponse.success(outlineWorkflowService.getWorkflow(workflowId));
    }

    @PostMapping("/outline-workflows/{workflowId}/commit")
    public ApiResponse<OutlineResponse> commitOutlineWorkflow(@PathVariable Long workflowId) {
        return ApiResponse.success("outline-workflow-committed", outlineWorkflowService.commitWorkflow(workflowId));
    }

    @GetMapping("/projects/{projectId}/global-outline")
    public ApiResponse<OutlineResponse> getGlobalOutline(@PathVariable Long projectId) {
        return ApiResponse.success(outlineService.getGlobalOutline(projectId));
    }

    @PatchMapping("/projects/{projectId}/global-outline")
    public ApiResponse<Void> updateGlobalOutline(
            @PathVariable Long projectId,
            @Valid @RequestBody OutlineUpdateRequest request) {
        outlineService.updateGlobalOutline(projectId, request);
        return ApiResponse.success("global-outline-updated", null);
    }

    @PatchMapping("/global-outlines/{outlineId}")
    public ApiResponse<Void> updateGlobalOutlineById(
            @PathVariable Long outlineId,
            @Valid @RequestBody OutlineUpdateRequest request) {
        outlineService.updateGlobalOutlineById(outlineId, request);
        return ApiResponse.success("global-outline-updated", null);
    }

    @PostMapping("/projects/{projectId}/global-outline/regenerate")
    public ApiResponse<OutlineResponse> rewriteGlobalOutline(
            @PathVariable Long projectId,
            @Valid @RequestBody OutlineRewriteRequest request) {
        return ApiResponse.success("global-outline-rewritten", outlineService.rewriteGlobalOutline(projectId, request));
    }

    @PostMapping("/projects/{projectId}/global-outline/confirm")
    public ApiResponse<Void> confirmGlobalOutline(@PathVariable Long projectId) {
        outlineService.confirmGlobalOutline(projectId);
        return ApiResponse.success("global-outline-confirmed", null);
    }

    @PostMapping("/global-outlines/{outlineId}/confirm")
    public ApiResponse<Void> confirmGlobalOutlineById(@PathVariable Long outlineId) {
        outlineService.confirmGlobalOutlineById(outlineId);
        return ApiResponse.success("global-outline-confirmed", null);
    }

    @PostMapping({"/projects/{projectId}/chapters/generate-outline", "/projects/{projectId}/chapter-outlines/generate"})
    public ApiResponse<List<ChapterResponse>> generateChapterOutlines(@PathVariable Long projectId) {
        return ApiResponse.success("chapter-outlines-generated", outlineService.generateChapterOutlines(projectId));
    }
}
