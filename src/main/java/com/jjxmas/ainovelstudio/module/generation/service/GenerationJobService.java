package com.jjxmas.ainovelstudio.module.generation.service;

import java.util.Map;

public interface GenerationJobService {

    Long recordFinishedJob(
            Long projectId,
            String jobType,
            String relatedEntityType,
            Long relatedEntityId,
            Long modelConfigId,
            Map<String, Object> input,
            Map<String, Object> output);
}

