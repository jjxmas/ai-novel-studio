package com.jjxmas.ainovelstudio.service;

import com.jjxmas.ainovelstudio.pojo.dto.ModelConfigCreateRequest;
import com.jjxmas.ainovelstudio.pojo.dto.ModelConfigResponse;
import com.jjxmas.ainovelstudio.pojo.dto.ModelConfigUpdateRequest;
import java.util.List;

/**
 * 模型配置服务，提供模型配置保存、查询、默认设置和禁用能力。
 */
public interface ModelConfigService {

    /**
     * 新增并保存模型配置。
     */
    Long saveModelConfig(ModelConfigCreateRequest request);

    /**
     * 更新指定模型配置。
     */
    void updateModelConfig(Long modelConfigId, ModelConfigUpdateRequest request);

    /**
     * 查询全部模型配置。
     */
    List<ModelConfigResponse> listModelConfigs();

    /**
     * 将指定模型配置设置为默认模型。
     */
    void setDefaultModel(Long modelConfigId);

    /**
     * 禁用指定模型配置。
     */
    void disableModelConfig(Long modelConfigId);
}
