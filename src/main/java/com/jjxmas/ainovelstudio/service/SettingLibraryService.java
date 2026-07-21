package com.jjxmas.ainovelstudio.service;

import com.jjxmas.ainovelstudio.pojo.dto.SettingLibraryGenerateRequest;
import com.jjxmas.ainovelstudio.pojo.dto.SettingLibraryResponse;
import com.jjxmas.ainovelstudio.pojo.dto.SettingLibraryRewriteRequest;
import com.jjxmas.ainovelstudio.pojo.dto.SettingLibraryUpdateRequest;

/**
 * 设定库服务，提供项目设定生成、维护、重写和确认能力。
 */
public interface SettingLibraryService {

    /**
     * 按请求生成项目设定库。
     */
    SettingLibraryResponse generateSettingLibrary(SettingLibraryGenerateRequest request);

    /**
     * 查询指定项目的设定库。
     */
    SettingLibraryResponse getSettingLibrary(Long projectId);

    /**
     * 按项目 ID 更新设定库内容。
     */
    SettingLibraryResponse updateSettingLibrary(Long projectId, SettingLibraryUpdateRequest request);

    /**
     * 按设定库 ID 更新设定库内容。
     */
    SettingLibraryResponse updateSettingLibraryById(Long settingLibraryId, SettingLibraryUpdateRequest request);

    /**
     * 根据重写指令重新生成设定库。
     */
    SettingLibraryResponse rewriteSettingLibrary(Long projectId, SettingLibraryRewriteRequest request);

    /**
     * 按项目 ID 确认设定库。
     */
    SettingLibraryResponse confirmSettingLibrary(Long projectId);

    /**
     * 按设定库 ID 确认设定库。
     */
    SettingLibraryResponse confirmSettingLibraryById(Long settingLibraryId);
}
