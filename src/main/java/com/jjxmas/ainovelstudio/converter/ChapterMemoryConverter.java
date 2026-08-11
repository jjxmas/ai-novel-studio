package com.jjxmas.ainovelstudio.converter;

import com.jjxmas.ainovelstudio.pojo.dto.ChapterSummaryResponse;
import com.jjxmas.ainovelstudio.pojo.dto.StoryMemoryResponse;
import com.jjxmas.ainovelstudio.pojo.entity.ChapterSummary;
import com.jjxmas.ainovelstudio.pojo.entity.StoryMemory;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ChapterMemoryConverter {

    StoryMemoryResponse toStoryMemoryResponse(StoryMemory memory);

    List<StoryMemoryResponse> toStoryMemoryResponseList(List<StoryMemory> memories);

    ChapterSummaryResponse toChapterSummaryResponse(ChapterSummary summary);

    List<ChapterSummaryResponse> toChapterSummaryResponseList(List<ChapterSummary> summaries);
}
