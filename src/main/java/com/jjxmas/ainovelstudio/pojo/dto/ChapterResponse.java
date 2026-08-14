package com.jjxmas.ainovelstudio.pojo.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapterResponse {

    private Long id;

    private Long projectId;

    private Integer chapterNo;

    private String title;

    private String outline;

    private List<String> scenePlan;

    private String content;

    private Integer wordCount;

    private String status;

    private Boolean outlineConfirmed;
}
