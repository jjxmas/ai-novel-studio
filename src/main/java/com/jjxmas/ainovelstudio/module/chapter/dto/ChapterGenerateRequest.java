package com.jjxmas.ainovelstudio.module.chapter.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 章节生成请求" */
@Data
public class ChapterGenerateRequest {

    @NotNull(message = "作品 ID 不能为空")
    private Long projectId;

    private Long chapterId;

    private Integer chapterNo;

    private String title;

    private String outline;

    private Long modelConfigId;

    private String revisionAdvice;
}

