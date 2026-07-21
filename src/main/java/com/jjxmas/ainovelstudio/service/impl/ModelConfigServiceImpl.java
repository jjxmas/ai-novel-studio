package com.jjxmas.ainovelstudio.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jjxmas.ainovelstudio.common.exception.BusinessException;
import com.jjxmas.ainovelstudio.common.exception.ErrorCode;
import com.jjxmas.ainovelstudio.pojo.dto.ModelConfigCreateRequest;
import com.jjxmas.ainovelstudio.pojo.dto.ModelConfigResponse;
import com.jjxmas.ainovelstudio.pojo.entity.ModelConfig;
import com.jjxmas.ainovelstudio.mapper.ModelConfigMapper;
import com.jjxmas.ainovelstudio.service.ModelConfigService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
/**
 * 模型配置服务实现，处理模型配置的保存、更新、默认选择和禁用。
 */
public class ModelConfigServiceImpl extends ServiceImpl<ModelConfigMapper, ModelConfig> implements ModelConfigService {

    /**
     * 保存新的模型配置，并在需要时清理旧默认模型。
     */
    @Override
    @Transactional
    public ModelConfigResponse saveModelConfig(ModelConfigCreateRequest request) {
        ModelConfig config = new ModelConfig()
                .setProvider(request.getProvider())
                .setDisplayName(request.getDisplayName())
                .setBaseUrl(trimToNull(request.getBaseUrl()))
                .setModelName(request.getModelName().trim())
                .setApiKeyCiphertext(normalizeApiKey(request.getApiKey()))
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

    /**
     * 更新指定模型配置，并同步默认模型和启用状态。
     */
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
                .setBaseUrl(trimToNull(request.getBaseUrl()))
                .setModelName(request.getModelName().trim())
                .setUsageType(request.getUsageType())
                .setEnabled(!Boolean.FALSE.equals(request.getEnabled()));
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
        return toResponse(config);
    }

    /**
     * 查询所有模型配置，默认模型和最近更新的配置优先展示。
     */
    @Override
    public List<ModelConfigResponse> listModelConfigs() {
        return list(new LambdaQueryWrapper<ModelConfig>().orderByDesc(ModelConfig::getDefaultModel).orderByDesc(ModelConfig::getUpdatedAt))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 将指定启用中的模型配置设为默认模型。
     */
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

    /**
     * 禁用指定模型配置，默认模型不允许直接禁用。
     */
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


    /**
     * 清除当前所有默认模型标记。
     */
    private void clearDefaultModel() {
        update(new LambdaUpdateWrapper<ModelConfig>()
                .eq(ModelConfig::getDefaultModel, true)
                .set(ModelConfig::getDefaultModel, false));
    }

    /**
     * 校验模型配置必填字段。
     */
    private void validateRequiredFields(ModelConfigCreateRequest request) {
        if (request.getProvider() == null || request.getProvider().isBlank()
                || request.getDisplayName() == null || request.getDisplayName().isBlank()
                || request.getModelName() == null || request.getModelName().isBlank()
                || request.getUsageType() == null || request.getUsageType().isBlank()) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "模型供应商、显示名称、模型名称和用途不能为空");
        }
    }

    /**
     * 规范化 API Key，去掉引号和 Bearer 前缀。
     */
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

    /**
     * 将空白字符串转换为 null，否则返回去空格后的文本。
     */
    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    /**
     * 将模型配置实体转换为接口响应对象。
     */
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
