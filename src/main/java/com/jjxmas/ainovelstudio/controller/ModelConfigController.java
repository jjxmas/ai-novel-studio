package com.jjxmas.ainovelstudio.controller;

import com.jjxmas.ainovelstudio.common.api.ApiResponse;
import com.jjxmas.ainovelstudio.pojo.dto.ModelConfigCreateRequest;
import com.jjxmas.ainovelstudio.pojo.dto.ModelConfigCreatedResponse;
import com.jjxmas.ainovelstudio.pojo.dto.ModelConfigResponse;
import com.jjxmas.ainovelstudio.pojo.dto.ModelConfigUpdateRequest;
import com.jjxmas.ainovelstudio.service.ModelConfigService;
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

/**
 * 模型配置接口，负责模型配置保存、修改、查询、默认模型设置和禁用。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/model-configs")
public class ModelConfigController {

    private final ModelConfigService modelConfigService;

    /**
     * 检查模型配置模块接口是否可用。
     */
    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.success("模型配置模块已就绪");
    }

    /**
     * 新增并保存模型配置。
     */
    @PostMapping
    public ApiResponse<ModelConfigCreatedResponse> saveModelConfig(@Valid @RequestBody ModelConfigCreateRequest request) {
        return ApiResponse.success("模型配置保存成功",
                new ModelConfigCreatedResponse(modelConfigService.saveModelConfig(request)));
    }

    /**
     * 更新指定模型配置。
     */
    @PatchMapping("/{modelConfigId}")
    public ApiResponse<Void> updateModelConfig(
            @PathVariable Long modelConfigId,
            @Valid @RequestBody ModelConfigUpdateRequest request) {
        modelConfigService.updateModelConfig(modelConfigId, request);
        return ApiResponse.success("模型配置修改成功", null);
    }

    /**
     * 查询全部模型配置。
     */
    @GetMapping
    public ApiResponse<List<ModelConfigResponse>> listModelConfigs() {
        return ApiResponse.success(modelConfigService.listModelConfigs());
    }

    /**
     * 将指定模型配置设为默认模型。
     */
    @PostMapping("/{modelConfigId}/default")
    public ApiResponse<Void> setDefaultModel(@PathVariable Long modelConfigId) {
        modelConfigService.setDefaultModel(modelConfigId);
        return ApiResponse.success("默认模型设置成功", null);
    }

    /**
     * 禁用指定模型配置。
     */
    @DeleteMapping("/{modelConfigId}")
    public ApiResponse<Void> disableModelConfig(@PathVariable Long modelConfigId) {
        modelConfigService.disableModelConfig(modelConfigId);
        return ApiResponse.success("模型配置已禁用", null);
    }
}
