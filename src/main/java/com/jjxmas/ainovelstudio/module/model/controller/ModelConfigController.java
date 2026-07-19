package com.jjxmas.ainovelstudio.module.model.controller;

import com.jjxmas.ainovelstudio.common.api.ApiResponse;
import com.jjxmas.ainovelstudio.module.model.dto.ModelConfigCreateRequest;
import com.jjxmas.ainovelstudio.module.model.dto.ModelConfigResponse;
import com.jjxmas.ainovelstudio.module.model.service.ModelConfigService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/model-configs")
public class ModelConfigController {

    private final ModelConfigService modelConfigService;

    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.success("模型配置模块已就绪");
    }

    @PostMapping
    public ApiResponse<ModelConfigResponse> saveModelConfig(@Valid @RequestBody ModelConfigCreateRequest request) {
        return ApiResponse.success("模型配置保存成功", modelConfigService.saveModelConfig(request));
    }

    @PatchMapping("/{modelConfigId}")
    public ApiResponse<ModelConfigResponse> updateModelConfig(
            @PathVariable Long modelConfigId,
            @RequestBody ModelConfigCreateRequest request) {
        return ApiResponse.success("模型配置修改成功", modelConfigService.updateModelConfig(modelConfigId, request));
    }

    @GetMapping
    public ApiResponse<List<ModelConfigResponse>> listModelConfigs() {
        return ApiResponse.success(modelConfigService.listModelConfigs());
    }

    @PostMapping("/{modelConfigId}/default")
    public ApiResponse<ModelConfigResponse> setDefaultModel(@PathVariable Long modelConfigId) {
        return ApiResponse.success("默认模型设置成功", modelConfigService.setDefaultModel(modelConfigId));
    }

    @DeleteMapping("/{modelConfigId}")
    public ApiResponse<ModelConfigResponse> disableModelConfig(@PathVariable Long modelConfigId) {
        return ApiResponse.success("模型配置已禁用", modelConfigService.disableModelConfig(modelConfigId));
    }
}
