package com.jjxmas.ainovelstudio.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jjxmas.ainovelstudio.common.exception.BusinessException;
import com.jjxmas.ainovelstudio.common.exception.ErrorCode;
import com.jjxmas.ainovelstudio.converter.ModelConfigConverter;
import com.jjxmas.ainovelstudio.mapper.ModelConfigMapper;
import com.jjxmas.ainovelstudio.pojo.dto.ModelConfigCreateRequest;
import com.jjxmas.ainovelstudio.pojo.dto.ModelConfigResponse;
import com.jjxmas.ainovelstudio.pojo.dto.ModelConfigUpdateRequest;
import com.jjxmas.ainovelstudio.pojo.entity.ModelConfig;
import com.jjxmas.ainovelstudio.service.ModelConfigService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ModelConfigServiceImpl extends ServiceImpl<ModelConfigMapper, ModelConfig> implements ModelConfigService {

    private final ModelConfigConverter modelConfigConverter;

    public ModelConfigServiceImpl(ModelConfigConverter modelConfigConverter) {
        this.modelConfigConverter = modelConfigConverter;
    }

    @Override
    @Transactional
    public Long saveModelConfig(ModelConfigCreateRequest request) {
        ModelConfig config = modelConfigConverter.toEntity(request)
                .setApiKeyCiphertext(normalizeApiKey(request.getApiKey()))
                .setDefaultModel(Boolean.TRUE.equals(request.getDefaultModel()))
                .setEnabled(!Boolean.FALSE.equals(request.getEnabled()))
                .setSupportsJson(true)
                .setSupportsStream(true);

        if (Boolean.TRUE.equals(config.getDefaultModel())) {
            clearDefaultModel();
        }
        save(config);
        return config.getId();
    }

    @Override
    @Transactional
    public void updateModelConfig(Long modelConfigId, ModelConfigUpdateRequest request) {
        ModelConfig config = requireModelConfig(modelConfigId);
        modelConfigConverter.updateEntity(request, config);
        config.setEnabled(!Boolean.FALSE.equals(request.getEnabled()));

        if (request.getApiKey() != null && !request.getApiKey().isBlank()) {
            config.setApiKeyCiphertext(normalizeApiKey(request.getApiKey()));
        }
        if (Boolean.TRUE.equals(request.getDefaultModel())) {
            clearDefaultModel();
            config.setDefaultModel(true);
            config.setEnabled(true);
        } else if (Boolean.FALSE.equals(config.getEnabled())) {
            config.setDefaultModel(false);
        }
        updateById(config);
    }

    @Override
    public List<ModelConfigResponse> listModelConfigs() {
        List<ModelConfig> configs = list(new LambdaQueryWrapper<ModelConfig>()
                .orderByDesc(ModelConfig::getDefaultModel)
                .orderByDesc(ModelConfig::getUpdatedAt));
        return modelConfigConverter.toResponseList(configs);
    }

    @Override
    @Transactional
    public void setDefaultModel(Long modelConfigId) {
        ModelConfig config = requireModelConfig(modelConfigId);
        if (Boolean.FALSE.equals(config.getEnabled())) {
            throw new BusinessException(ErrorCode.MODEL_CONFIG_INVALID, "模型配置不存在或未启用");
        }
        clearDefaultModel();
        config.setDefaultModel(true);
        updateById(config);
    }

    @Override
    @Transactional
    public void disableModelConfig(Long modelConfigId) {
        ModelConfig config = requireModelConfig(modelConfigId);
        if (Boolean.TRUE.equals(config.getDefaultModel())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "默认模型不能直接禁用，请先设置其他默认模型");
        }
        config.setEnabled(false);
        updateById(config);
    }

    private void clearDefaultModel() {
        update(new LambdaUpdateWrapper<ModelConfig>()
                .eq(ModelConfig::getDefaultModel, true)
                .set(ModelConfig::getDefaultModel, false));
    }

    private ModelConfig requireModelConfig(Long modelConfigId) {
        ModelConfig config = getById(modelConfigId);
        if (config == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "模型配置不存在");
        }
        return config;
    }

    private String normalizeApiKey(String apiKey) {
        String normalized = apiKey.trim();
        if ((normalized.startsWith("\"") && normalized.endsWith("\""))
                || (normalized.startsWith("'") && normalized.endsWith("'"))) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }
        if (normalized.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) {
            normalized = normalized.substring("Bearer ".length()).trim();
        }
        return normalized;
    }
}
