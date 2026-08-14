package com.jjxmas.ainovelstudio.pojo.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChapterQualityCheckResult {

    private String status;
    private Integer issueCount;
    private CheckResponse report;
    private String errorMessage;
}
