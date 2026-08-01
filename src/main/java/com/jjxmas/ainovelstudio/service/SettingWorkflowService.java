package com.jjxmas.ainovelstudio.service;

import com.jjxmas.ainovelstudio.pojo.dto.SettingLibraryResponse;
import com.jjxmas.ainovelstudio.pojo.dto.SettingWorkflowCreateRequest;
import com.jjxmas.ainovelstudio.pojo.dto.SettingWorkflowResponse;

public interface SettingWorkflowService {

    SettingWorkflowResponse startWorkflow(SettingWorkflowCreateRequest request);

    SettingWorkflowResponse getWorkflow(Long workflowId);

    SettingWorkflowResponse getLatestWorkflow(Long projectId);

    SettingWorkflowResponse approveBlueprint(Long workflowId);

    SettingWorkflowResponse regenerateModule(Long workflowId, String moduleKey);

    SettingLibraryResponse commitWorkflow(Long workflowId);
}
