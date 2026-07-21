package com.jjxmas.ainovelstudio.pojo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 检查请求。
 */
@Data
public class CheckRequest {

    @NotNull(message = "作品 ID 不能为空")
    private Long projectId;

    private Long chapterId;

    @NotNull(message = "检查类型不能为空")
    private String checkType;

    private String targetText;
}
