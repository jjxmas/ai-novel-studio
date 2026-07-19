package com.jjxmas.ainovelstudio.module.memory.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StoryMemoryResponse {

    private Long id;

    private String memoryType;

    private String memoryKey;

    private Integer sequenceNo;

    private Integer startChapterNo;

    private Integer endChapterNo;

    private String content;

    private String status;

    private Boolean current;
}
