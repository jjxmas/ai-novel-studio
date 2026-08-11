package com.jjxmas.ainovelstudio.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jjxmas.ainovelstudio.common.exception.BusinessException;
import com.jjxmas.ainovelstudio.common.exception.ErrorCode;
import com.jjxmas.ainovelstudio.converter.ProjectConverter;
import com.jjxmas.ainovelstudio.pojo.dto.ProjectCreateRequest;
import com.jjxmas.ainovelstudio.pojo.dto.ProjectResponse;
import com.jjxmas.ainovelstudio.pojo.entity.Project;
import com.jjxmas.ainovelstudio.mapper.ProjectMapper;
import com.jjxmas.ainovelstudio.service.ProjectService;
import com.jjxmas.ainovelstudio.service.VersionService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 作品项目服务实现，处理项目持久化和响应转换。
 */
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements ProjectService {

    private final VersionService versionService;
    private final ProjectConverter projectConverter;

    /**
     * 创建作品项目并保存初始项目状态。
     */
    @Override
    @Transactional
    public Long createProject(ProjectCreateRequest request) {
        Project project = new Project();
        projectConverter.updateEntity(request, project);
        applyDefaults(project);
        save(project);
        return project.getId();
    }

    /**
     * 修改作品基础信息并记录项目版本快照。
     */
    @Override
    @Transactional
    public void updateProject(Long projectId, ProjectCreateRequest request) {
        Project project = requireProject(projectId);
        projectConverter.updateEntity(request, project);
        applyDefaults(project);
        updateById(project);
        versionService.recordVersion(
                project.getId(),
                "project",
                project.getId(),
                projectSnapshot(project),
                "user_edit",
                "用户修改作品基础信息",
                null,
                null);
    }

    /**
     * 查询指定项目并转换为接口响应。
     */
    @Override
    public ProjectResponse getProject(Long projectId) {
        return toResponse(requireProject(projectId));
    }

    /**
     * 按更新时间倒序查询项目列表。
     */
    @Override
    public List<ProjectResponse> listProjects() {
        return projectConverter.toResponseList(list(new LambdaQueryWrapper<Project>()
                .orderByDesc(Project::getUpdatedAt)));
    }

    /**
     * 获取项目实体，不存在时抛出业务异常。
     */
    private Project requireProject(Long projectId) {
        Project project = getById(projectId);
        if (project == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "作品不存在");
        }
        return project;
    }

    /**
     * 将项目实体转换为项目响应对象。
     */
    private ProjectResponse toResponse(Project project) {
        return projectConverter.toResponse(project);
    }

    private void applyDefaults(Project project) {
        project.setTargetWordCountMin(defaultNumber(project.getTargetWordCountMin()))
                .setTargetWordCountMax(defaultNumber(project.getTargetWordCountMax()))
                .setTargetChapterWordCount(defaultChapterWordCount(project.getTargetChapterWordCount()))
                .setPlatformTarget(defaultText(project.getPlatformTarget(), "general"));
        if (project.getStatus() == null) {
            project.setStatus("drafting");
        }
    }

    /**
     * 生成作品基础信息版本快照。
     */
    private Map<String, Object> projectSnapshot(Project project) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("title", project.getTitle());
        snapshot.put("genres", project.getGenres() == null ? List.of() : project.getGenres());
        snapshot.put("targetWordCountMin", project.getTargetWordCountMin());
        snapshot.put("targetWordCountMax", project.getTargetWordCountMax());
        snapshot.put("targetChapterWordCount", project.getTargetChapterWordCount());
        snapshot.put("platformTarget", project.getPlatformTarget());
        snapshot.put("stylePreference", project.getStylePreference());
        snapshot.put("projectBrief", project.getProjectBrief());
        snapshot.put("status", project.getStatus());
        return snapshot;
    }

    /**
     * 为可空数字提供默认值。
     */
    private Integer defaultNumber(Integer value) {
        return value == null ? 0 : value;
    }

    private Integer defaultChapterWordCount(Integer value) {
        return value == null || value <= 0 ? 3000 : value;
    }

    /**
     * 为空白文本提供默认值。
     */
    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
