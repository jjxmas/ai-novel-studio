package com.jjxmas.ainovelstudio.module.model.service;

import com.jjxmas.ainovelstudio.module.model.dto.ModelConfigCreateRequest;
import com.jjxmas.ainovelstudio.module.model.dto.ModelConfigResponse;
import java.util.List;

public interface ModelConfigService {

    ModelConfigResponse saveModelConfig(ModelConfigCreateRequest request);

    ModelConfigResponse updateModelConfig(Long modelConfigId, ModelConfigCreateRequest request);

    List<ModelConfigResponse> listModelConfigs();

    ModelConfigResponse setDefaultModel(Long modelConfigId);

    ModelConfigResponse disableModelConfig(Long modelConfigId);
}

