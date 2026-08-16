package com.jjxmas.ainovelstudio.service;

import com.jjxmas.ainovelstudio.pojo.dto.ProjectCreateRequest;
import com.jjxmas.ainovelstudio.pojo.dto.ProjectResponse;
import java.util.List;

/**
 * 作品项目服务，提供项目创建和查询能力。
 */
public interface ProjectService {

    /**
     * 创建新的小说作品项目。
     */
    Long createProject(ProjectCreateRequest request);

    /**
     * 修改指定作品的基础信息。
     */
    void updateProject(Long projectId, ProjectCreateRequest request);

    /**
     * 删除指定作品及其关联创作数据。
     */
    void deleteProject(Long projectId);

    /**
     * 查询指定项目详情。
     */
    ProjectResponse getProject(Long projectId);

    /**
     * 查询全部作品项目列表。
     */
    List<ProjectResponse> listProjects();
}
