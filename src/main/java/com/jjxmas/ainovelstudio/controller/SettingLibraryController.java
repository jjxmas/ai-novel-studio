package com.jjxmas.ainovelstudio.controller;

import com.jjxmas.ainovelstudio.common.api.ApiResponse;
import com.jjxmas.ainovelstudio.pojo.dto.SettingLibraryGenerateRequest;
import com.jjxmas.ainovelstudio.pojo.dto.SettingLibraryResponse;
import com.jjxmas.ainovelstudio.pojo.dto.SettingLibraryRewriteRequest;
import com.jjxmas.ainovelstudio.pojo.dto.SettingLibraryUpdateRequest;
import com.jjxmas.ainovelstudio.service.SettingLibraryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 设定库接口，负责生成、查询、更新、重写和确认项目设定。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class SettingLibraryController {

    private final SettingLibraryService settingLibraryService;

    /**
     * 检查设定库模块接口是否可用。
     */
    @GetMapping("/setting-library/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.success("设定库模块已就绪");
    }

    /**
     * 为指定项目生成设定库。
     */
    @PostMapping("/projects/{projectId}/setting-library/generate")
    public ApiResponse<SettingLibraryResponse> generateSettingLibrary(
            @PathVariable Long projectId,
            @Valid @RequestBody SettingLibraryGenerateRequest request) {
        request.setProjectId(projectId);
        return ApiResponse.success("设定库生成完成", settingLibraryService.generateSettingLibrary(request));
    }

    /**
     * 查询指定项目的设定库。
     */
    @GetMapping("/projects/{projectId}/setting-library")
    public ApiResponse<SettingLibraryResponse> getSettingLibrary(@PathVariable Long projectId) {
        return ApiResponse.success(settingLibraryService.getSettingLibrary(projectId));
    }

    /**
     * 按项目 ID 更新设定库内容。
     */
    @PatchMapping("/projects/{projectId}/setting-library")
    public ApiResponse<SettingLibraryResponse> updateSettingLibrary(
            @PathVariable Long projectId,
            @Valid @RequestBody SettingLibraryUpdateRequest request) {
        return ApiResponse.success("设定库修改已保存", settingLibraryService.updateSettingLibrary(projectId, request));
    }

    /**
     * 按设定库 ID 更新设定库内容。
     */
    @PatchMapping("/setting-library/{settingLibraryId}")
    public ApiResponse<SettingLibraryResponse> updateSettingLibraryById(
            @PathVariable Long settingLibraryId,
            @Valid @RequestBody SettingLibraryUpdateRequest request) {
        return ApiResponse.success("设定库修改已保存", settingLibraryService.updateSettingLibraryById(settingLibraryId, request));
    }

    /**
     * 根据修改指令重新生成设定库。
     */
    @PostMapping("/projects/{projectId}/setting-library/regenerate")
    public ApiResponse<SettingLibraryResponse> rewriteSettingLibrary(
            @PathVariable Long projectId,
            @Valid @RequestBody SettingLibraryRewriteRequest request) {
        return ApiResponse.success("设定库重生成完成", settingLibraryService.rewriteSettingLibrary(projectId, request));
    }

    /**
     * 按项目 ID 确认设定库。
     */
    @PostMapping("/projects/{projectId}/setting-library/confirm")
    public ApiResponse<SettingLibraryResponse> confirmSettingLibrary(@PathVariable Long projectId) {
        return ApiResponse.success("设定库已确认", settingLibraryService.confirmSettingLibrary(projectId));
    }

    /**
     * 按设定库 ID 确认设定库。
     */
    @PostMapping("/setting-library/{settingLibraryId}/confirm")
    public ApiResponse<SettingLibraryResponse> confirmSettingLibraryById(@PathVariable Long settingLibraryId) {
        return ApiResponse.success("设定库已确认", settingLibraryService.confirmSettingLibraryById(settingLibraryId));
    }
}
