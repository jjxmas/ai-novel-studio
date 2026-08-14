package com.jjxmas.ainovelstudio.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChapterGenerationResult {

    private ChapterResponse chapter;

    private Long generationJobId;

    private ChapterQualityCheckResult qualityCheck;
}
