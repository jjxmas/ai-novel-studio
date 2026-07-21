package com.jjxmas.ainovelstudio.controller;

import com.jjxmas.ainovelstudio.common.api.ApiResponse;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterResponse;
import com.jjxmas.ainovelstudio.pojo.dto.OutlineGenerateRequest;
import com.jjxmas.ainovelstudio.pojo.dto.OutlineResponse;
import com.jjxmas.ainovelstudio.pojo.dto.OutlineRewriteRequest;
import com.jjxmas.ainovelstudio.pojo.dto.OutlineUpdateRequest;
import com.jjxmas.ainovelstudio.service.OutlineService;
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

/**
 * 大纲接口，负责全局大纲生成、维护、确认和章节大纲生成。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class OutlineController {

    private final OutlineService outlineService;

    /**
     * 检查大纲模块接口是否可用。
     */
    @GetMapping("/global-outline/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.success("大纲模块已就绪");
    }

    /**
     * 为指定项目生成全局大纲。
     */
    @PostMapping("/projects/{projectId}/global-outline/generate")
    public ApiResponse<OutlineResponse> generateGlobalOutline(
            @PathVariable Long projectId,
            @Valid @RequestBody OutlineGenerateRequest request) {
        request.setProjectId(projectId);
        request.setOutlineLevel("global");
        return ApiResponse.success("全局大纲生成完成", outlineService.generateOutline(request));
    }

    /**
     * 查询指定项目的全局大纲。
     */
    @GetMapping("/projects/{projectId}/global-outline")
    public ApiResponse<OutlineResponse> getGlobalOutline(@PathVariable Long projectId) {
        return ApiResponse.success(outlineService.getGlobalOutline(projectId));
    }

    /**
     * 按项目 ID 更新全局大纲。
     */
    @PatchMapping("/projects/{projectId}/global-outline")
    public ApiResponse<OutlineResponse> updateGlobalOutline(
            @PathVariable Long projectId,
            @Valid @RequestBody OutlineUpdateRequest request) {
        return ApiResponse.success("全局大纲修改已保存", outlineService.updateGlobalOutline(projectId, request));
    }

    /**
     * 按大纲 ID 更新全局大纲。
     */
    @PatchMapping("/global-outlines/{outlineId}")
    public ApiResponse<OutlineResponse> updateGlobalOutlineById(
            @PathVariable Long outlineId,
            @Valid @RequestBody OutlineUpdateRequest request) {
        return ApiResponse.success("全局大纲修改已保存", outlineService.updateGlobalOutlineById(outlineId, request));
    }

    /**
     * 根据修改指令重新生成全局大纲。
     */
    @PostMapping("/projects/{projectId}/global-outline/regenerate")
    public ApiResponse<OutlineResponse> rewriteGlobalOutline(
            @PathVariable Long projectId,
            @Valid @RequestBody OutlineRewriteRequest request) {
        return ApiResponse.success("全局大纲重生成完成", outlineService.rewriteGlobalOutline(projectId, request));
    }

    /**
     * 按项目 ID 确认全局大纲。
     */
    @PostMapping("/projects/{projectId}/global-outline/confirm")
    public ApiResponse<OutlineResponse> confirmGlobalOutline(@PathVariable Long projectId) {
        return ApiResponse.success("全局大纲已确认", outlineService.confirmGlobalOutline(projectId));
    }

    /**
     * 按大纲 ID 确认全局大纲。
     */
    @PostMapping("/global-outlines/{outlineId}/confirm")
    public ApiResponse<OutlineResponse> confirmGlobalOutlineById(@PathVariable Long outlineId) {
        return ApiResponse.success("全局大纲已确认", outlineService.confirmGlobalOutlineById(outlineId));
    }

    /**
     * 基于已确认的全局大纲生成卷和章节大纲。
     */
    @PostMapping({"/projects/{projectId}/chapters/generate-outline", "/projects/{projectId}/chapter-outlines/generate"})
    public ApiResponse<List<ChapterResponse>> generateChapterOutlines(@PathVariable Long projectId) {
        return ApiResponse.success("分卷与章节大纲生成完成", outlineService.generateChapterOutlines(projectId));
    }
}
