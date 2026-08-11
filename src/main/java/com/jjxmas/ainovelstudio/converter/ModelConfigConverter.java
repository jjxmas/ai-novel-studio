package com.jjxmas.ainovelstudio.converter;

import com.jjxmas.ainovelstudio.pojo.dto.ModelConfigCreateRequest;
import com.jjxmas.ainovelstudio.pojo.dto.ModelConfigResponse;
import com.jjxmas.ainovelstudio.pojo.dto.ModelConfigUpdateRequest;
import com.jjxmas.ainovelstudio.pojo.entity.ModelConfig;
import java.util.List;

import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ModelConfigConverter {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "apiKeyCiphertext", ignore = true)
    @Mapping(target = "defaultModel", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "contextWindow", ignore = true)
    @Mapping(target = "temperature", ignore = true)
    @Mapping(target = "topP", ignore = true)
    @Mapping(target = "supportsJson", ignore = true)
    @Mapping(target = "supportsStream", ignore = true)
    @Mapping(target = "notes", ignore = true)
    @Mapping(target = "provider", source = "provider", qualifiedByName = "trim")
    @Mapping(target = "displayName", source = "displayName", qualifiedByName = "trim")
    @Mapping(target = "baseUrl", source = "baseUrl", qualifiedByName = "trimToNull")
    @Mapping(target = "modelName", source = "modelName", qualifiedByName = "trim")
    @Mapping(target = "usageType", source = "usageType", qualifiedByName = "trim")
    ModelConfig toEntity(ModelConfigCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "apiKeyCiphertext", ignore = true)
    @Mapping(target = "defaultModel", ignore = true)
    @Mapping(target = "contextWindow", ignore = true)
    @Mapping(target = "temperature", ignore = true)
    @Mapping(target = "topP", ignore = true)
    @Mapping(target = "supportsJson", ignore = true)
    @Mapping(target = "supportsStream", ignore = true)
    @Mapping(target = "notes", ignore = true)
    @Mapping(target = "provider", source = "provider", qualifiedByName = "trim")
    @Mapping(target = "displayName", source = "displayName", qualifiedByName = "trim")
    @Mapping(target = "baseUrl", source = "baseUrl", qualifiedByName = "trimToNull")
    @Mapping(target = "modelName", source = "modelName", qualifiedByName = "trim")
    @Mapping(target = "usageType", source = "usageType", qualifiedByName = "trim")
    void updateEntity(ModelConfigUpdateRequest request, @MappingTarget ModelConfig target);

    @Mapping(target = "hasApiKey", expression = "java(hasText(config.getApiKeyCiphertext()))")
    ModelConfigResponse toResponse(ModelConfig config);

    List<ModelConfigResponse> toResponseList(List<ModelConfig> configs);

    @Named("trim")
    default String trim(String value) {
        return value == null ? null : value.trim();
    }

    @Named("trimToNull")
    default String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    default boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
