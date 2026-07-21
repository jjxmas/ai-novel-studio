package com.jjxmas.ainovelstudio.pojo.dto;

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

    private Integer chapterNo;

    private String title;

    private String outline;

    private String content;

    private Integer wordCount;

    private String status;

    private Boolean outlineConfirmed;
}

