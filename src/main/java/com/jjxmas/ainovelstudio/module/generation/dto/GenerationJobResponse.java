package com.jjxmas.ainovelstudio.module.generation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 生成任务占位响应。第一阶段只固定异步任务契约，不启动真实模型。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerationJobResponse {

    private Long taskId;

    private Long projectId;

    private String taskType;

    private String status;

    private String message;

    public static GenerationJobResponse queued(Long projectId, String taskType) {
        return GenerationJobResponse.builder()
                .taskId(null)
                .projectId(projectId)
                .taskType(taskType)
                .status("queued")
                .message("任务已按异步生成流程预留，真实模型调用将在后续阶段接入")
                .build();
    }
}
