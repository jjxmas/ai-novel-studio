package com.jjxmas.ainovelstudio.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

/**
 * 创意生成请求。第三版通过后端 AI 编排服务生成，失败时回退 mock。
 */
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
