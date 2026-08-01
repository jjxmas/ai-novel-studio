package com.jjxmas.ainovelstudio.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SettingLibraryUpdateRequest {

    @NotBlank(message = "设定库内容不能为空")
    private String summary;

    private String overview;

    private String genreTemplate;

    private String changeNote;
}
