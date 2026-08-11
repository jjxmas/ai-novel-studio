package com.jjxmas.ainovelstudio.converter;

import com.jjxmas.ainovelstudio.pojo.dto.OutlineWorkflowResponse;
import com.jjxmas.ainovelstudio.pojo.entity.OutlineWorkflowRun;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OutlineWorkflowConverter {

    @Mapping(target = "draft", ignore = true)
    @Mapping(target = "checks", ignore = true)
    OutlineWorkflowResponse toResponse(OutlineWorkflowRun run);
}
