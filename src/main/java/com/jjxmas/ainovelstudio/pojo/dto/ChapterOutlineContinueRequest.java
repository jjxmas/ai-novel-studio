package com.jjxmas.ainovelstudio.pojo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChapterOutlineContinueRequest {

    @NotNull(message = "续写章节数量不能为空")
    private Integer count;

    private Long modelConfigId;

    private String instruction;
}
