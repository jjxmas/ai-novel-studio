package com.jjxmas.ainovelstudio.service.impl;

import com.jjxmas.ainovelstudio.common.exception.BusinessException;
import com.jjxmas.ainovelstudio.common.exception.ErrorCode;
import com.jjxmas.ainovelstudio.common.util.JsonUtils;
import com.jjxmas.ainovelstudio.pojo.entity.GenerationJob;
import com.jjxmas.ainovelstudio.mapper.GenerationJobMapper;
import com.jjxmas.ainovelstudio.service.GenerationJobService;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
/**
 * 生成任务服务实现，负责落库 AI 生成任务记录。
 */
public class GenerationJobServiceImpl implements GenerationJobService {

    private final GenerationJobMapper generationJobMapper;

    /**
     * 注入生成任务 Mapper。
     */
    public GenerationJobServiceImpl(GenerationJobMapper generationJobMapper) {
        this.generationJobMapper = generationJobMapper;
    }

    @Override
    public Long enqueueJob(
            Long projectId,
            String jobType,
            String relatedEntityType,
            Long relatedEntityId,
            Long modelConfigId,
            Map<String, Object> input,
            String dedupeKey,
            int priority,
            LocalDateTime scheduledAt) {
        GenerationJob job = new GenerationJob()
                .setProjectId(projectId)
                .setJobType(jobType)
                .setRelatedEntityType(relatedEntityType)
                .setRelatedEntityId(relatedEntityId)
                .setDedupeKey(dedupeKey)
                .setModelConfigId(modelConfigId)
                .setStatus("pending")
                .setPriority(priority)
                .setAttemptCount(0)
                .setInputSnapshot(JsonUtils.toJson(input == null ? Map.of() : input))
                .setScheduledAt(scheduledAt);
        generationJobMapper.insertPending(job);
        return job.getId();
    }

    @Override
    @Transactional
    public GenerationJob claimNext(String workerId, LocalDateTime now) {
        requireWorkerId(workerId);
        LocalDateTime claimedAt = requireTime(now);
        GenerationJob job = generationJobMapper.selectNextClaimable(claimedAt);
        if (job == null) {
            return null;
        }
        job.setStatus("running")
                .setLockedBy(workerId)
                .setLockedAt(claimedAt)
                .setStartedAt(job.getStartedAt() == null ? claimedAt : job.getStartedAt())
                .setAttemptCount(value(job.getAttemptCount()) + 1);
        generationJobMapper.updateById(job);
        return job;
    }

    @Override
    public void heartbeat(Long jobId, String workerId, LocalDateTime now) {
        requireWorkerId(workerId);
        if (generationJobMapper.heartbeat(jobId, workerId, requireTime(now)) != 1) {
            throw claimConflict();
        }
    }

    @Override
    public void completeJob(Long jobId, String workerId, Map<String, Object> output, LocalDateTime now) {
        requireWorkerId(workerId);
        String outputSnapshot = JsonUtils.toJson(output == null ? Map.of() : output);
        if (generationJobMapper.completeClaim(jobId, workerId, outputSnapshot, requireTime(now)) != 1) {
            throw claimConflict();
        }
    }

    @Override
    public void failJob(
            Long jobId,
            String workerId,
            String errorMessage,
            boolean retry,
            LocalDateTime scheduledAt,
            LocalDateTime now) {
        requireWorkerId(workerId);
        LocalDateTime failedAt = requireTime(now);
        String nextStatus = retry ? "pending" : "failed";
        LocalDateTime nextAttemptAt = retry ? (scheduledAt == null ? failedAt : scheduledAt) : null;
        LocalDateTime finishedAt = retry ? null : failedAt;
        if (generationJobMapper.failClaim(
                        jobId,
                        workerId,
                        nextStatus,
                        errorMessage == null ? "" : errorMessage,
                        nextAttemptAt,
                        finishedAt)
                != 1) {
            throw claimConflict();
        }
    }

    @Override
    public int recoverExpiredJobs(LocalDateTime lockedBefore, LocalDateTime now) {
        if (lockedBefore == null) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "JOB_LEASE_CUTOFF_REQUIRED");
        }
        return generationJobMapper.recoverExpired(lockedBefore, requireTime(now));
    }

    /**
     * 记录一条已完成的生成任务并返回任务 ID。
     */
    @Override
    public Long recordFinishedJob(
            Long projectId,
            String jobType,
            String relatedEntityType,
            Long relatedEntityId,
            Long modelConfigId,
            Map<String, Object> input,
            Map<String, Object> output) {
        LocalDateTime now = LocalDateTime.now();
        GenerationJob job = baseJob(projectId, jobType, relatedEntityType, relatedEntityId, modelConfigId, input, now)
                .setStatus("succeeded")
                .setOutputSnapshot(JsonUtils.toJson(output));
        generationJobMapper.insert(job);
        return job.getId();
    }

    @Override
    public Long recordFailedJob(
            Long projectId,
            String jobType,
            String relatedEntityType,
            Long relatedEntityId,
            Long modelConfigId,
            Map<String, Object> input,
            String errorMessage) {
        LocalDateTime now = LocalDateTime.now();
        GenerationJob job = baseJob(projectId, jobType, relatedEntityType, relatedEntityId, modelConfigId, input, now)
                .setStatus("failed")
                .setErrorMessage(errorMessage == null ? "" : errorMessage);
        generationJobMapper.insert(job);
        return job.getId();
    }

    private GenerationJob baseJob(
            Long projectId,
            String jobType,
            String relatedEntityType,
            Long relatedEntityId,
            Long modelConfigId,
            Map<String, Object> input,
            LocalDateTime now) {
        return new GenerationJob()
                .setProjectId(projectId)
                .setJobType(jobType)
                .setRelatedEntityType(relatedEntityType)
                .setRelatedEntityId(relatedEntityId)
                .setModelConfigId(modelConfigId)
                .setPriority(0)
                .setAttemptCount(1)
                .setInputSnapshot(JsonUtils.toJson(input))
                .setStartedAt(now)
                .setFinishedAt(now);
    }

    private void requireWorkerId(String workerId) {
        if (workerId == null || workerId.isBlank()) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "WORKER_ID_REQUIRED");
        }
    }

    private LocalDateTime requireTime(LocalDateTime time) {
        if (time == null) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "JOB_TIME_REQUIRED");
        }
        return time;
    }

    private BusinessException claimConflict() {
        return new BusinessException(ErrorCode.BUSINESS_ERROR, "GENERATION_JOB_CLAIM_CONFLICT");
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }
}
