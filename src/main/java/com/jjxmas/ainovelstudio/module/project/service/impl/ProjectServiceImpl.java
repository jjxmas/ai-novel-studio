package com.jjxmas.ainovelstudio.module.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jjxmas.ainovelstudio.common.exception.BusinessException;
import com.jjxmas.ainovelstudio.common.exception.ErrorCode;
import com.jjxmas.ainovelstudio.common.util.JsonUtils;
import com.jjxmas.ainovelstudio.module.project.dto.ProjectCreateRequest;
import com.jjxmas.ainovelstudio.module.project.dto.ProjectResponse;
import com.jjxmas.ainovelstudio.module.project.entity.Project;
import com.jjxmas.ainovelstudio.module.project.mapper.ProjectMapper;
import com.jjxmas.ainovelstudio.module.project.service.ProjectService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements ProjectService {

    @Override
    @Transactional
    public ProjectResponse createProject(ProjectCreateRequest request) {
        Project project = new Project()
                .setTitle(request.getTitle())
                .setGenres(JsonUtils.toJson(request.getGenres()))
                .setTargetWordCountMin(defaultNumber(request.getTargetWordCountMin()))
                .setTargetWordCountMax(defaultNumber(request.getTargetWordCountMax()))
                .setPlatformTarget(defaultText(request.getPlatformTarget(), "general"))
                .setStylePreference(request.getStylePreference())
                .setProjectBrief(request.getProjectBrief())
                .setStatus("drafting");
        save(project);
        return toResponse(project);
    }

    @Override
    public ProjectResponse getProject(Long projectId) {
        return toResponse(requireProject(projectId));
    }

    @Override
    public List<ProjectResponse> listProjects() {
        return list(new LambdaQueryWrapper<Project>().orderByDesc(Project::getUpdatedAt))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private Project requireProject(Long projectId) {
        Project project = getById(projectId);
        if (project == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "作品不存在");
        }
        return project;
    }

    private ProjectResponse toResponse(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .title(project.getTitle())
                .genres(JsonUtils.toStringList(project.getGenres()))
                .targetWordCountMin(project.getTargetWordCountMin())
                .targetWordCountMax(project.getTargetWordCountMax())
                .platformTarget(project.getPlatformTarget())
                .stylePreference(project.getStylePreference())
                .projectBrief(project.getProjectBrief())
                .status(project.getStatus())
                .selectedIdeaId(project.getSelectedIdeaId())
                .build();
    }

    private Integer defaultNumber(Integer value) {
        return value == null ? 0 : value;
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
