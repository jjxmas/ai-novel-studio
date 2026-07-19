package com.jjxmas.ainovelstudio.module.idea.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.Data;

@Data
public class IdeaUpdateRequest {

    @NotBlank(message = "创意标题不能为空")
    private String title;

    private List<String> sellingPoints;

    @NotBlank(message = "世界观不能为空")
    private String worldview;

    @NotBlank(message = "主线冲突不能为空")
    private String mainConflict;

    private Integer estimatedWordCount;

    @NotBlank(message = "创意摘要不能为空")
    private String summary;

    private String changeNote;
}
