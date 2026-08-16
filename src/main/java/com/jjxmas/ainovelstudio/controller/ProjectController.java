package com.jjxmas.ainovelstudio.controller;

import com.jjxmas.ainovelstudio.common.api.ApiResponse;
import com.jjxmas.ainovelstudio.pojo.dto.ProjectCreateRequest;
import com.jjxmas.ainovelstudio.pojo.dto.ProjectResponse;
import com.jjxmas.ainovelstudio.pojo.dto.StoryDirtyMarkSnapshotResponse;
import com.jjxmas.ainovelstudio.pojo.dto.StoryRebuildRequest;
import com.jjxmas.ainovelstudio.pojo.dto.StoryRebuildResult;
import com.jjxmas.ainovelstudio.service.ProjectService;
import com.jjxmas.ainovelstudio.service.StoryDirtyMarkService;
import com.jjxmas.ainovelstudio.service.StoryRebuildService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final StoryDirtyMarkService storyDirtyMarkService;
    private final StoryRebuildService storyRebuildService;

    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.success("project module ready");
    }

    @PostMapping
    public ApiResponse<Long> createProject(@Valid @RequestBody ProjectCreateRequest request) {
        return ApiResponse.success(projectService.createProject(request));
    }

    @PatchMapping("/{projectId}")
    public ApiResponse<Void> updateProject(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectCreateRequest request) {
        projectService.updateProject(projectId, request);
        return ApiResponse.success((Void) null);
    }

    @DeleteMapping("/{projectId}")
    public ApiResponse<Void> deleteProject(@PathVariable Long projectId) {
        projectService.deleteProject(projectId);
        return ApiResponse.success((Void) null);
    }

    @GetMapping("/{projectId}")
    public ApiResponse<ProjectResponse> getProject(@PathVariable Long projectId) {
        return ApiResponse.success(projectService.getProject(projectId));
    }

    @GetMapping
    public ApiResponse<List<ProjectResponse>> listProjects() {
        return ApiResponse.success(projectService.listProjects());
    }

    @GetMapping("/{projectId}/story-dirty-marks")
    public ApiResponse<StoryDirtyMarkSnapshotResponse> getStoryDirtyMarks(
            @PathVariable Long projectId,
            @RequestParam(required = false) Integer chapterNo) {
        return ApiResponse.success(storyDirtyMarkService.activeSnapshot(projectId, chapterNo));
    }

    @PostMapping("/{projectId}/story-rebuild")
    public ApiResponse<StoryRebuildResult> rebuildStoryState(
            @PathVariable Long projectId,
            @Valid @RequestBody StoryRebuildRequest request) {
        return ApiResponse.success(storyRebuildService.rebuildFromChapter(
                projectId,
                request == null ? null : request.getStartChapterNo(),
                request == null ? null : request.getModelConfigId()));
    }
}
