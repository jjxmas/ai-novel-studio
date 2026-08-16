package com.jjxmas.ainovelstudio.pojo.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 章节目录项，不包含可能很大的正文。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapterCatalogResponse {

    private Long id;
    private Long projectId;
    private Integer chapterNo;
    private String title;
    private String outline;
    private List<String> scenePlan;
    private Integer wordCount;
    private String status;
    private String contentStatus;
    private LocalDateTime contentGeneratedAt;
    private LocalDateTime contentUpdatedAt;
    private Long lastGenerationJobId;
    private Integer lastContentVersionNo;
    private Boolean outlineConfirmed;
    private Boolean hasContent;
}
