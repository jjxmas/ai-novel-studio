package com.jjxmas.ainovelstudio.module.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

/**
 * 新建作品请求。第一阶段只校验最核心字段，后续再补落库规则。
 */
@Data
public class ProjectCreateRequest {

    @NotBlank(message = "作品名称不能为空")
    private String title;

    @NotEmpty(message = "至少选择一个小说类型")
    private List<String> genres;

    private Integer targetWordCountMin;

    private Integer targetWordCountMax;

    private String platformTarget;

    private String stylePreference;

    @NotBlank(message = "模糊描述不能为空")
    private String projectBrief;
}
