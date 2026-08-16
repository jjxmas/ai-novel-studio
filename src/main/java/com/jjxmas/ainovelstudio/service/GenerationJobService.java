package com.jjxmas.ainovelstudio.service;

import com.jjxmas.ainovelstudio.pojo.entity.GenerationJob;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 生成任务服务，负责记录 AI 生成任务执行结果。
 */
public interface GenerationJobService {

    Long enqueueJob(
            Long projectId,
            String jobType,
            String relatedEntityType,
            Long relatedEntityId,
            Long modelConfigId,
            Map<String, Object> input,
            String dedupeKey,
            int priority,
            LocalDateTime scheduledAt);

    GenerationJob claimNext(String workerId, LocalDateTime now);

    void heartbeat(Long jobId, String workerId, LocalDateTime now);

    void completeJob(Long jobId, String workerId, Map<String, Object> output, LocalDateTime now);

    void failJob(
            Long jobId,
            String workerId,
            String errorMessage,
            boolean retry,
            LocalDateTime scheduledAt,
            LocalDateTime now);

    int recoverExpiredJobs(LocalDateTime lockedBefore, LocalDateTime now);

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
