package com.jjxmas.ainovelstudio.converter;

import com.jjxmas.ainovelstudio.pojo.dto.ProjectCreateRequest;
import com.jjxmas.ainovelstudio.pojo.dto.ProjectResponse;
import com.jjxmas.ainovelstudio.pojo.entity.Project;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProjectConverter {

    ProjectResponse toResponse(Project project);

    List<ProjectResponse> toResponseList(List<Project> projects);

    void updateEntity(ProjectCreateRequest request, @MappingTarget Project project);
}
