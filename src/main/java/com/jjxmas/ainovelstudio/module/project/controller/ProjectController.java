package com.jjxmas.ainovelstudio.module.project.controller;

import com.jjxmas.ainovelstudio.common.api.ApiResponse;
import com.jjxmas.ainovelstudio.module.project.dto.ProjectCreateRequest;
import com.jjxmas.ainovelstudio.module.project.dto.ProjectResponse;
import com.jjxmas.ainovelstudio.module.project.service.ProjectService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.success("作品模块已就绪");
    }

    @PostMapping
    public ApiResponse<ProjectResponse> createProject(@Valid @RequestBody ProjectCreateRequest request) {
        return ApiResponse.success("作品创建成功", projectService.createProject(request));
    }

    @GetMapping("/{projectId}")
    public ApiResponse<ProjectResponse> getProject(@PathVariable Long projectId) {
        return ApiResponse.success(projectService.getProject(projectId));
    }

    @GetMapping
    public ApiResponse<List<ProjectResponse>> listProjects() {
        return ApiResponse.success(projectService.listProjects());
    }
}
