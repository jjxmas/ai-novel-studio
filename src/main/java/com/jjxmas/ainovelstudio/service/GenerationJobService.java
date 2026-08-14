package com.jjxmas.ainovelstudio.service;

import java.util.Map;

/**
 * 生成任务服务，负责记录 AI 生成任务执行结果。
 */
public interface GenerationJobService {

    /**
     * 记录一条已完成的生成任务并返回任务 ID。
     */
    Long recordFinishedJob(
            Long projectId,
            String jobType,
            String relatedEntityType,
            Long relatedEntityId,
            Long modelConfigId,
            Map<String, Object> input,
            Map<String, Object> output);

    Long recordFailedJob(
            Long projectId,
            String jobType,
            String relatedEntityType,
            Long relatedEntityId,
            Long modelConfigId,
            Map<String, Object> input,
            String errorMessage);
}
