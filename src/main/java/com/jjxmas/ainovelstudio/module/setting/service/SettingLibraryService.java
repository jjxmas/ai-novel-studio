package com.jjxmas.ainovelstudio.module.setting.service;

import com.jjxmas.ainovelstudio.module.setting.dto.SettingLibraryGenerateRequest;
import com.jjxmas.ainovelstudio.module.setting.dto.SettingLibraryResponse;
import com.jjxmas.ainovelstudio.module.setting.dto.SettingLibraryRewriteRequest;
import com.jjxmas.ainovelstudio.module.setting.dto.SettingLibraryUpdateRequest;

public interface SettingLibraryService {

    SettingLibraryResponse generateSettingLibrary(SettingLibraryGenerateRequest request);

    SettingLibraryResponse getSettingLibrary(Long projectId);

    SettingLibraryResponse updateSettingLibrary(Long projectId, SettingLibraryUpdateRequest request);

    SettingLibraryResponse updateSettingLibraryById(Long settingLibraryId, SettingLibraryUpdateRequest request);

    SettingLibraryResponse rewriteSettingLibrary(Long projectId, SettingLibraryRewriteRequest request);

    SettingLibraryResponse confirmSettingLibrary(Long projectId);

    SettingLibraryResponse confirmSettingLibraryById(Long settingLibraryId);
}

