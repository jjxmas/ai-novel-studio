package com.jjxmas.ainovelstudio.module.model.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jjxmas.ainovelstudio.common.exception.BusinessException;
import com.jjxmas.ainovelstudio.common.exception.ErrorCode;
import com.jjxmas.ainovelstudio.module.model.dto.ModelConfigCreateRequest;
import com.jjxmas.ainovelstudio.module.model.dto.ModelConfigResponse;
import com.jjxmas.ainovelstudio.module.model.entity.ModelConfig;
import com.jjxmas.ainovelstudio.module.model.mapper.ModelConfigMapper;
import com.jjxmas.ainovelstudio.module.model.service.ModelConfigService;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ModelConfigServiceImpl extends ServiceImpl<ModelConfigMapper, ModelConfig> implements ModelConfigService {

    @Override
    @Transactional
    public ModelConfigResponse saveModelConfig(ModelConfigCreateRequest request) {
        ModelConfig config = new ModelConfig()
                .setProvider(request.getProvider())
                .setDisplayName(request.getDisplayName())
                .setBaseUrl(request.getBaseUrl())
                .setModelName(request.getModelName())
                .setApiKeyCiphertext(encodeApiKey(request.getApiKey()))
                .setUsageType(request.getUsageType())
                .setDefaultModel(Boolean.TRUE.equals(request.getDefaultModel()))
                .setEnabled(!Boolean.FALSE.equals(request.getEnabled()))
                .setSupportsJson(true)
                .setSupportsStream(true);

        if (Boolean.TRUE.equals(config.getDefaultModel())) {
            clearDefaultModel();
        }
        save(config);
        return toResponse(config);
    }

    @Override
    @Transactional
    public ModelConfigResponse updateModelConfig(Long modelConfigId, ModelConfigCreateRequest request) {
        ModelConfig config = getById(modelConfigId);
        if (config == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "模型配置不存在");
        }
        validateRequiredFields(request);
        config.setProvider(request.getProvider())
                .setDisplayName(request.getDisplayName())
                .setBaseUrl(request.getBaseUrl())
                .setModelName(request.getModelName())
                .setUsageType(request.getUsageType())
                .setEnabled(!Boolean.FALSE.equals(request.getEnabled()));
        if (request.getApiKey() != null && !request.getApiKey().isBlank()) {
            config.setApiKeyCiphertext(encodeApiKey(request.getApiKey()));
        }
        if (Boolean.TRUE.equals(request.getDefaultModel())) {
            clearDefaultModel();
            config.setDefaultModel(true);
            config.setEnabled(true);
        } else if (Boolean.FALSE.equals(config.getEnabled())) {
            config.setDefaultModel(false);
        }
        updateById(config);
        return toResponse(config);
    }

    @Override
    public List<ModelConfigResponse> listModelConfigs() {
        return list(new LambdaQueryWrapper<ModelConfig>().orderByDesc(ModelConfig::getDefaultModel).orderByDesc(ModelConfig::getUpdatedAt))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ModelConfigResponse setDefaultModel(Long modelConfigId) {
        ModelConfig config = getById(modelConfigId);
        if (config == null || Boolean.FALSE.equals(config.getEnabled())) {
            throw new BusinessException(ErrorCode.MODEL_CONFIG_INVALID, "模型配置不存在或未启用");
        }
        clearDefaultModel();
        config.setDefaultModel(true);
        updateById(config);
        return toResponse(config);
    }

    @Override
    @Transactional
    public ModelConfigResponse disableModelConfig(Long modelConfigId) {
        ModelConfig config = getById(modelConfigId);
        if (config == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "模型配置不存在");
        }
        if (Boolean.TRUE.equals(config.getDefaultModel())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "默认模型不能直接禁用，请先设置其他默认模型");
        }
        config.setEnabled(false);
        updateById(config);
        return toResponse(config);
    }


    private void clearDefaultModel() {
        update(new LambdaUpdateWrapper<ModelConfig>()
                .eq(ModelConfig::getDefaultModel, true)
                .set(ModelConfig::getDefaultModel, false));
    }

    private void validateRequiredFields(ModelConfigCreateRequest request) {
        if (request.getProvider() == null || request.getProvider().isBlank()
                || request.getDisplayName() == null || request.getDisplayName().isBlank()
                || request.getModelName() == null || request.getModelName().isBlank()
                || request.getUsageType() == null || request.getUsageType().isBlank()) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "模型供应商、显示名称、模型名称和用途不能为空");
        }
    }

    private String encodeApiKey(String apiKey) {
        return Base64.getEncoder().encodeToString(apiKey.getBytes(StandardCharsets.UTF_8));
    }

    private ModelConfigResponse toResponse(ModelConfig config) {
        return ModelConfigResponse.builder()
                .id(config.getId())
                .provider(config.getProvider())
                .displayName(config.getDisplayName())
                .baseUrl(config.getBaseUrl())
                .modelName(config.getModelName())
                .usageType(config.getUsageType())
                .hasApiKey(config.getApiKeyCiphertext() != null && !config.getApiKeyCiphertext().isBlank())
                .defaultModel(config.getDefaultModel())
                .enabled(config.getEnabled())
                .build();
    }
}
