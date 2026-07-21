package com.jjxmas.ainovelstudio.service.impl;

import com.jjxmas.ainovelstudio.common.util.JsonUtils;
import com.jjxmas.ainovelstudio.pojo.entity.GenerationJob;
import com.jjxmas.ainovelstudio.mapper.GenerationJobMapper;
import com.jjxmas.ainovelstudio.service.GenerationJobService;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.stereotype.Service;

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
        GenerationJob job = new GenerationJob()
                .setProjectId(projectId)
                .setJobType(jobType)
                .setRelatedEntityType(relatedEntityType)
                .setRelatedEntityId(relatedEntityId)
                .setModelConfigId(modelConfigId)
                .setStatus("finished")
                .setPriority(0)
                .setAttemptCount(1)
                .setInputSnapshot(JsonUtils.toJson(input))
                .setOutputSnapshot(JsonUtils.toJson(output))
                .setStartedAt(now)
                .setFinishedAt(now);
        generationJobMapper.insert(job);
        return job.getId();
    }
}
