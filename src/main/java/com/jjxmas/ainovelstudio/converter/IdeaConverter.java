package com.jjxmas.ainovelstudio.converter;

import com.jjxmas.ainovelstudio.pojo.dto.IdeaResponse;
import com.jjxmas.ainovelstudio.pojo.entity.Idea;
import com.jjxmas.ainovelstudio.pojo.entity.IdeaEvaluation;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface IdeaConverter {

    @Mapping(target = "id", source = "idea.id")
    @Mapping(target = "title", source = "idea.title")
    @Mapping(target = "sellingPoints", source = "idea.sellingPoints")
    @Mapping(target = "worldview", source = "idea.worldview")
    @Mapping(target = "mainConflict", source = "idea.mainConflict")
    @Mapping(target = "estimatedWordCount", source = "idea.estimatedWordCount")
    @Mapping(target = "summary", source = "idea.summary")
    @Mapping(target = "status", source = "idea.status")
    @Mapping(target = "longFormPotentialScore", expression = "java(score(evaluation == null ? null : evaluation.getLongFormPotentialScore()))")
    @Mapping(target = "conflictScore", expression = "java(score(evaluation == null ? null : evaluation.getConflictScore()))")
    @Mapping(target = "noveltyScore", expression = "java(score(evaluation == null ? null : evaluation.getNoveltyScore()))")
    @Mapping(target = "beginnerFriendlinessScore", expression = "java(score(evaluation == null ? null : evaluation.getBeginnerFriendlinessScore()))")
    @Mapping(target = "platformFitScore", expression = "java(score(evaluation == null ? null : evaluation.getPlatformFitScore()))")
    @Mapping(target = "riskLevel", expression = "java(evaluation == null ? null : evaluation.getRiskLevel())")
    @Mapping(target = "strengths", expression = "java(defaultList(evaluation == null ? null : evaluation.getStrengths()))")
    @Mapping(target = "risks", expression = "java(defaultList(evaluation == null ? null : evaluation.getRisks()))")
    @Mapping(target = "suggestions", expression = "java(defaultList(evaluation == null ? null : evaluation.getSuggestions()))")
    @Mapping(target = "overallComment", expression = "java(defaultText(evaluation == null ? null : evaluation.getOverallComment()))")
    IdeaResponse toResponse(Idea idea, IdeaEvaluation evaluation);

    default Integer score(Double value) {
        return value == null ? null : (int) Math.round(value);
    }

    default <T> List<T> defaultList(List<T> values) {
        return values == null ? List.of() : values;
    }

    default String defaultText(String value) {
        return value == null ? "" : value;
    }
}
