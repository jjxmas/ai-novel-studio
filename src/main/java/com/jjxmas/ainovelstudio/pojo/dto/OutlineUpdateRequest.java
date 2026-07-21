package com.jjxmas.ainovelstudio.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OutlineUpdateRequest {

    @NotBlank(message = "大纲标题不能为空")
    private String title;

    @NotBlank(message = "大纲内容不能为空")
    private String content;

    private String changeNote;
}

