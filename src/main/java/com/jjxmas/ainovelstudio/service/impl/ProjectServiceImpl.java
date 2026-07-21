package com.jjxmas.ainovelstudio.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jjxmas.ainovelstudio.common.exception.BusinessException;
import com.jjxmas.ainovelstudio.common.exception.ErrorCode;
import com.jjxmas.ainovelstudio.common.util.JsonUtils;
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

@Service
@RequiredArgsConstructor
/**
 * 作品项目服务实现，处理项目持久化和响应转换。
 */
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements ProjectService {

    private final VersionService versionService;

    /**
     * 创建作品项目并保存初始项目状态。
     */
    @Override
    @Transactional
    public ProjectResponse createProject(ProjectCreateRequest request) {
        Project project = new Project()
                .setTitle(request.getTitle())
                .setGenres(JsonUtils.toJson(request.getGenres()))
                .setTargetWordCountMin(defaultNumber(request.getTargetWordCountMin()))
                .setTargetWordCountMax(defaultNumber(request.getTargetWordCountMax()))
                .setTargetChapterWordCount(defaultChapterWordCount(request.getTargetChapterWordCount()))
                .setPlatformTarget(defaultText(request.getPlatformTarget(), "general"))
                .setStylePreference(request.getStylePreference())
                .setProjectBrief(request.getProjectBrief())
                .setStatus("drafting");
        save(project);
        return toResponse(project);
    }

    /**
     * 修改作品基础信息并记录项目版本快照。
     */
    @Override
    @Transactional
    public ProjectResponse updateProject(Long projectId, ProjectCreateRequest request) {
        Project project = requireProject(projectId);
        project
                .setTitle(request.getTitle())
                .setGenres(JsonUtils.toJson(request.getGenres()))
                .setTargetWordCountMin(defaultNumber(request.getTargetWordCountMin()))
                .setTargetWordCountMax(defaultNumber(request.getTargetWordCountMax()))
                .setTargetChapterWordCount(defaultChapterWordCount(request.getTargetChapterWordCount()))
                .setPlatformTarget(defaultText(request.getPlatformTarget(), "general"))
                .setStylePreference(request.getStylePreference())
                .setProjectBrief(request.getProjectBrief());
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
        return toResponse(project);
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
        return list(new LambdaQueryWrapper<Project>().orderByDesc(Project::getUpdatedAt))
                .stream()
                .map(this::toResponse)
                .toList();
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
        return ProjectResponse.builder()
                .id(project.getId())
                .title(project.getTitle())
                .genres(JsonUtils.toStringList(project.getGenres()))
                .targetWordCountMin(project.getTargetWordCountMin())
                .targetWordCountMax(project.getTargetWordCountMax())
                .targetChapterWordCount(project.getTargetChapterWordCount())
                .platformTarget(project.getPlatformTarget())
                .stylePreference(project.getStylePreference())
                .projectBrief(project.getProjectBrief())
                .status(project.getStatus())
                .selectedIdeaId(project.getSelectedIdeaId())
                .build();
    }

    /**
     * 生成作品基础信息版本快照。
     */
    private Map<String, Object> projectSnapshot(Project project) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("title", project.getTitle());
        snapshot.put("genres", JsonUtils.toStringList(project.getGenres()));
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
