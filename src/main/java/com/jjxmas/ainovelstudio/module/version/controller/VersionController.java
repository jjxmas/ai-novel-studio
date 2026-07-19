package com.jjxmas.ainovelstudio.module.version.controller;

import com.jjxmas.ainovelstudio.common.api.ApiResponse;
import com.jjxmas.ainovelstudio.module.version.dto.VersionResponse;
import com.jjxmas.ainovelstudio.module.version.service.VersionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 版本管理模块入口。第一阶段只保留查询入口，回滚和对比后续实现" */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/versions")
public class VersionController {

    private final VersionService versionService;

    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.success("版本管理模块已就绪");
    }

    @GetMapping("/{versionId}")
    public ApiResponse<VersionResponse> getVersion(@PathVariable Long versionId) {
        return ApiResponse.success(versionService.getVersion(versionId));
    }

    @GetMapping
    public ApiResponse<List<VersionResponse>> listVersions(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId) {
        return ApiResponse.success(versionService.listVersions(projectId, entityType, entityId));
    }
}
