package com.jjxmas.ainovelstudio.converter;

import com.jjxmas.ainovelstudio.pojo.dto.ChapterResponse;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterCatalogResponse;
import com.jjxmas.ainovelstudio.pojo.entity.Chapter;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ChapterConverter {

    @Mapping(target = "outlineConfirmed", expression = "java(chapter.getConfirmedOutlineAt() != null)")
    @Mapping(
            target = "scenePlan",
            expression = "java(com.jjxmas.ainovelstudio.common.util.JsonUtils.toStringList(chapter.getScenePlan()))")
    ChapterResponse toResponse(Chapter chapter);

    List<ChapterResponse> toResponseList(List<Chapter> chapters);

    @Mapping(target = "outlineConfirmed", expression = "java(chapter.getConfirmedOutlineAt() != null)")
    @Mapping(target = "hasContent", expression = "java(chapter.getContentStatus() != null && !\"not_generated\".equals(chapter.getContentStatus()))")
    @Mapping(
            target = "scenePlan",
            expression = "java(com.jjxmas.ainovelstudio.common.util.JsonUtils.toStringList(chapter.getScenePlan()))")
    ChapterCatalogResponse toCatalogResponse(Chapter chapter);

    List<ChapterCatalogResponse> toCatalogResponseList(List<Chapter> chapters);
}
