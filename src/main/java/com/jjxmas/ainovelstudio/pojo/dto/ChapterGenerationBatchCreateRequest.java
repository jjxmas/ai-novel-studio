package com.jjxmas.ainovelstudio.pojo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChapterGenerationBatchCreateRequest {

    @NotNull(message = "起始章节不能为空")
    @Min(value = 1, message = "起始章节必须大于 0")
    private Integer startChapterNo;

    @NotNull(message = "章节数量不能为空")
    @Min(value = 1, message = "章节数量必须大于 0")
    @Max(value = 50, message = "单个批次最多生成 50 章")
    private Integer count;

    private Long modelConfigId;

    @NotNull(message = "是否跳过已有正文不能为空")
    private Boolean skipExistingContent;

    private String instruction;
}
