package com.jjxmas.ainovelstudio.pojo.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class StoryRebuildRequest {

    @Min(value = 1, message = "起始章节必须大于 0")
    private Integer startChapterNo;

    private Long modelConfigId;
}
