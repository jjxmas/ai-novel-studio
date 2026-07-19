package com.jjxmas.ainovelstudio.module.chapter.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChapterContentUpdateRequest {

    @NotBlank(message = "章节正文不能为空")
    private String content;

    private String changeNote;
}

