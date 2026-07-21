package com.jjxmas.ainovelstudio.controller;

import com.jjxmas.ainovelstudio.common.api.ApiResponse;
import com.jjxmas.ainovelstudio.pojo.dto.ProjectCreateRequest;
import com.jjxmas.ainovelstudio.pojo.dto.ProjectResponse;
import com.jjxmas.ainovelstudio.service.ProjectService;
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

/**
 * 作品项目接口，负责项目创建、查询和列表入口。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projectService;

    /**
     * 检查作品模块接口是否可用。
     */
    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.success("作品模块已就绪");
    }

    /**
     * 创建新的小说作品项目。
     */
    @PostMapping
    public ApiResponse<ProjectResponse> createProject(@Valid @RequestBody ProjectCreateRequest request) {
        return ApiResponse.success("作品创建成功", projectService.createProject(request));
    }

    /**
     * 修改小说作品项目的基础信息。
     */
    @PatchMapping("/{projectId}")
    public ApiResponse<ProjectResponse> updateProject(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectCreateRequest request) {
        return ApiResponse.success("作品修改成功", projectService.updateProject(projectId, request));
    }

    /**
     * 按项目 ID 查询作品项目详情。
     */
    @GetMapping("/{projectId}")
    public ApiResponse<ProjectResponse> getProject(@PathVariable Long projectId) {
        return ApiResponse.success(projectService.getProject(projectId));
    }

    /**
     * 查询全部作品项目列表。
     */
    @GetMapping
    public ApiResponse<List<ProjectResponse>> listProjects() {
        return ApiResponse.success(projectService.listProjects());
    }
}
