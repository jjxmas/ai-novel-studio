package com.jjxmas.ainovelstudio.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StoryEventUpsertRequest {

    @NotBlank(message = "事件名称不能为空")
    private String name;

    private String eventType;

    private String description;

    private String eventTimeText;

    private Long locationId;

    private Long chapterId;

    private Boolean planned;

    private Integer importance;
}
