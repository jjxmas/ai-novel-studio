package com.jjxmas.ainovelstudio.converter;

import com.jjxmas.ainovelstudio.pojo.dto.VersionResponse;
import com.jjxmas.ainovelstudio.pojo.entity.ContentVersion;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface VersionConverter {

    VersionResponse toResponse(ContentVersion version);

    List<VersionResponse> toResponseList(List<ContentVersion> versions);
}
