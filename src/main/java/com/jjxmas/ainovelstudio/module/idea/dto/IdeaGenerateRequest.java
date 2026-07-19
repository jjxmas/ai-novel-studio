package com.jjxmas.ainovelstudio.module.idea.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

/**
 * 创意生成请求。真实模型调用后续通过 NovelAiClient 接入" */
@Data
public class IdeaGenerateRequest {

    @NotNull(message = "作品 ID 不能为空")
    private Long projectId;

    private Long modelConfigId;

    private List<String> genres;

    @NotBlank(message = "创意描述不能为空")
    private String briefDescription;

    private Integer ideaCount;
}

