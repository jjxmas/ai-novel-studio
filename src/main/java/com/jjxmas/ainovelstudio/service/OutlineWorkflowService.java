package com.jjxmas.ainovelstudio.service;

import com.jjxmas.ainovelstudio.pojo.dto.OutlineWorkflowCreateRequest;
import com.jjxmas.ainovelstudio.pojo.dto.OutlineWorkflowResponse;
import com.jjxmas.ainovelstudio.pojo.dto.OutlineResponse;

public interface OutlineWorkflowService {

    OutlineWorkflowResponse startWorkflow(OutlineWorkflowCreateRequest request);

    OutlineWorkflowResponse getWorkflow(Long workflowId);

    OutlineWorkflowResponse getLatestWorkflow(Long projectId);

    OutlineResponse commitWorkflow(Long workflowId);
}
