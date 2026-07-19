package com.jjxmas.ainovelstudio.module.project.service;

import com.jjxmas.ainovelstudio.module.project.dto.ProjectCreateRequest;
import com.jjxmas.ainovelstudio.module.project.dto.ProjectResponse;
import java.util.List;

public interface ProjectService {

    ProjectResponse createProject(ProjectCreateRequest request);

    ProjectResponse getProject(Long projectId);

    List<ProjectResponse> listProjects();
}

