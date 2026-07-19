package com.jjxmas.ainovelstudio.module.outline.controller;

import com.jjxmas.ainovelstudio.common.api.ApiResponse;
import com.jjxmas.ainovelstudio.module.chapter.dto.ChapterResponse;
import com.jjxmas.ainovelstudio.module.outline.dto.OutlineGenerateRequest;
import com.jjxmas.ainovelstudio.module.outline.dto.OutlineResponse;
import com.jjxmas.ainovelstudio.module.outline.dto.OutlineRewriteRequest;
import com.jjxmas.ainovelstudio.module.outline.dto.OutlineUpdateRequest;
import com.jjxmas.ainovelstudio.module.outline.service.OutlineService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class OutlineController {

    private final OutlineService outlineService;

    @GetMapping("/global-outline/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.success("大纲模块已就绪");
    }

    @PostMapping("/projects/{projectId}/global-outline/generate")
    public ApiResponse<OutlineResponse> generateGlobalOutline(
            @PathVariable Long projectId,
            @Valid @RequestBody OutlineGenerateRequest request) {
        request.setProjectId(projectId);
        request.setOutlineLevel("global");
        return ApiResponse.success("全局大纲生成完成", outlineService.generateOutline(request));
    }

    @GetMapping("/projects/{projectId}/global-outline")
    public ApiResponse<OutlineResponse> getGlobalOutline(@PathVariable Long projectId) {
        return ApiResponse.success(outlineService.getGlobalOutline(projectId));
    }

    @PatchMapping("/projects/{projectId}/global-outline")
    public ApiResponse<OutlineResponse> updateGlobalOutline(
            @PathVariable Long projectId,
            @Valid @RequestBody OutlineUpdateRequest request) {
        return ApiResponse.success("全局大纲修改已保存", outlineService.updateGlobalOutline(projectId, request));
    }

    @PatchMapping("/global-outlines/{outlineId}")
    public ApiResponse<OutlineResponse> updateGlobalOutlineById(
            @PathVariable Long outlineId,
            @Valid @RequestBody OutlineUpdateRequest request) {
        return ApiResponse.success("全局大纲修改已保存", outlineService.updateGlobalOutlineById(outlineId, request));
    }

    @PostMapping("/projects/{projectId}/global-outline/regenerate")
    public ApiResponse<OutlineResponse> rewriteGlobalOutline(
            @PathVariable Long projectId,
            @Valid @RequestBody OutlineRewriteRequest request) {
        return ApiResponse.success("全局大纲重生成完成", outlineService.rewriteGlobalOutline(projectId, request));
    }

    @PostMapping("/projects/{projectId}/global-outline/confirm")
    public ApiResponse<OutlineResponse> confirmGlobalOutline(@PathVariable Long projectId) {
        return ApiResponse.success("全局大纲已确认", outlineService.confirmGlobalOutline(projectId));
    }

    @PostMapping("/global-outlines/{outlineId}/confirm")
    public ApiResponse<OutlineResponse> confirmGlobalOutlineById(@PathVariable Long outlineId) {
        return ApiResponse.success("全局大纲已确认", outlineService.confirmGlobalOutlineById(outlineId));
    }

    @PostMapping({"/projects/{projectId}/chapters/generate-outline", "/projects/{projectId}/chapter-outlines/generate"})
    public ApiResponse<List<ChapterResponse>> generateChapterOutlines(@PathVariable Long projectId) {
        return ApiResponse.success("分卷与章节大纲生成完成", outlineService.generateChapterOutlines(projectId));
    }
}
