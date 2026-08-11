package com.jjxmas.ainovelstudio.converter;

import com.jjxmas.ainovelstudio.pojo.dto.OutlineResponse;
import com.jjxmas.ainovelstudio.pojo.dto.VolumeOutlineResponse;
import com.jjxmas.ainovelstudio.pojo.entity.Outline;
import com.jjxmas.ainovelstudio.pojo.entity.Volume;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OutlineConverter {

    @Mapping(target = "outlineLevel", constant = "global")
    @Mapping(target = "confirmed", expression = "java(outline.getConfirmedAt() != null)")
    OutlineResponse toResponse(Outline outline);

    VolumeOutlineResponse toVolumeResponse(Volume volume);

    List<VolumeOutlineResponse> toVolumeResponseList(List<Volume> volumes);
}
