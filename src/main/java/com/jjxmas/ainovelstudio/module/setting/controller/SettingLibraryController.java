package com.jjxmas.ainovelstudio.module.setting.controller;

import com.jjxmas.ainovelstudio.common.api.ApiResponse;
import com.jjxmas.ainovelstudio.module.setting.dto.SettingLibraryGenerateRequest;
import com.jjxmas.ainovelstudio.module.setting.dto.SettingLibraryResponse;
import com.jjxmas.ainovelstudio.module.setting.dto.SettingLibraryRewriteRequest;
import com.jjxmas.ainovelstudio.module.setting.dto.SettingLibraryUpdateRequest;
import com.jjxmas.ainovelstudio.module.setting.service.SettingLibraryService;
import jakarta.validation.Valid;
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
public class SettingLibraryController {

    private final SettingLibraryService settingLibraryService;

    @GetMapping("/setting-library/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.success("设定库模块已就绪");
    }

    @PostMapping("/projects/{projectId}/setting-library/generate")
    public ApiResponse<SettingLibraryResponse> generateSettingLibrary(
            @PathVariable Long projectId,
            @Valid @RequestBody SettingLibraryGenerateRequest request) {
        request.setProjectId(projectId);
        return ApiResponse.success("设定库生成完成", settingLibraryService.generateSettingLibrary(request));
    }

    @GetMapping("/projects/{projectId}/setting-library")
    public ApiResponse<SettingLibraryResponse> getSettingLibrary(@PathVariable Long projectId) {
        return ApiResponse.success(settingLibraryService.getSettingLibrary(projectId));
    }

    @PatchMapping("/projects/{projectId}/setting-library")
    public ApiResponse<SettingLibraryResponse> updateSettingLibrary(
            @PathVariable Long projectId,
            @Valid @RequestBody SettingLibraryUpdateRequest request) {
        return ApiResponse.success("设定库修改已保存", settingLibraryService.updateSettingLibrary(projectId, request));
    }

    @PatchMapping("/setting-library/{settingLibraryId}")
    public ApiResponse<SettingLibraryResponse> updateSettingLibraryById(
            @PathVariable Long settingLibraryId,
            @Valid @RequestBody SettingLibraryUpdateRequest request) {
        return ApiResponse.success("设定库修改已保存", settingLibraryService.updateSettingLibraryById(settingLibraryId, request));
    }

    @PostMapping("/projects/{projectId}/setting-library/regenerate")
    public ApiResponse<SettingLibraryResponse> rewriteSettingLibrary(
            @PathVariable Long projectId,
            @Valid @RequestBody SettingLibraryRewriteRequest request) {
        return ApiResponse.success("设定库重生成完成", settingLibraryService.rewriteSettingLibrary(projectId, request));
    }

    @PostMapping("/projects/{projectId}/setting-library/confirm")
    public ApiResponse<SettingLibraryResponse> confirmSettingLibrary(@PathVariable Long projectId) {
        return ApiResponse.success("设定库已确认", settingLibraryService.confirmSettingLibrary(projectId));
    }

    @PostMapping("/setting-library/{settingLibraryId}/confirm")
    public ApiResponse<SettingLibraryResponse> confirmSettingLibraryById(@PathVariable Long settingLibraryId) {
        return ApiResponse.success("设定库已确认", settingLibraryService.confirmSettingLibraryById(settingLibraryId));
    }
}
