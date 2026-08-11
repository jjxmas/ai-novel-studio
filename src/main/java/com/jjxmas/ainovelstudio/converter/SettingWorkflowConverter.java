package com.jjxmas.ainovelstudio.converter;

import com.jjxmas.ainovelstudio.pojo.dto.SettingWorkflowResponse;
import com.jjxmas.ainovelstudio.pojo.entity.SettingWorkflowRun;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SettingWorkflowConverter {

    @Mapping(target = "blueprint", ignore = true)
    @Mapping(target = "draft", ignore = true)
    @Mapping(target = "checks", ignore = true)
    SettingWorkflowResponse toResponse(SettingWorkflowRun run);
}
