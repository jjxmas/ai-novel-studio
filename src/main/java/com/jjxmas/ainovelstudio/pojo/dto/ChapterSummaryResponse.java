package com.jjxmas.ainovelstudio.pojo.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChapterSummaryResponse {

    private Long id;

    private Long chapterId;

    private Integer chapterNo;

    private String summary;
}
