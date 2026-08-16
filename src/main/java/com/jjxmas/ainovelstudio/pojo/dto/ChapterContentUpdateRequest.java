package com.jjxmas.ainovelstudio.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChapterContentUpdateRequest {

    @NotBlank(message = "章节正文不能为空")
    private String content;

    @NotNull(message = "正文版本号不能为空")
    private Integer expectedVersion;

    private String changeNote;
}
