package com.jjxmas.ainovelstudio.module.generation.service.impl;

import com.jjxmas.ainovelstudio.common.util.JsonUtils;
import com.jjxmas.ainovelstudio.module.generation.entity.GenerationJob;
import com.jjxmas.ainovelstudio.module.generation.mapper.GenerationJobMapper;
import com.jjxmas.ainovelstudio.module.generation.service.GenerationJobService;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class GenerationJobServiceImpl implements GenerationJobService {

    private final GenerationJobMapper generationJobMapper;

    public GenerationJobServiceImpl(GenerationJobMapper generationJobMapper) {
        this.generationJobMapper = generationJobMapper;
    }

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

