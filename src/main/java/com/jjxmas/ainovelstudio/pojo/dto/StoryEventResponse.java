package com.jjxmas.ainovelstudio.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryEventResponse {

    private Long id;

    private Long projectId;

    private String name;

    private String eventType;

    private String description;

    private String eventTimeText;

    private Long locationId;

    private Long chapterId;

    private Boolean planned;

    private Integer importance;
}
