package com.jjxmas.ainovelstudio.module.project.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {

    private Long id;

    private String title;

    private List<String> genres;

    private Integer targetWordCountMin;

    private Integer targetWordCountMax;

    private String platformTarget;

    private String stylePreference;

    private String projectBrief;

    private String status;

    private Long selectedIdeaId;
}

