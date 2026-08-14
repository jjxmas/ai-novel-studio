package com.jjxmas.ainovelstudio.pojo.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryDirtyMarkResponse {

    private Long id;

    private Long projectId;

    private String sourceType;

    private Long sourceId;

    private Long sourceChapterId;

    private Integer sourceChapterNo;

    private Integer dirtyFromChapterNo;

    private String dirtyScope;

    private String reasonType;

    private String reasonNote;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime resolvedAt;
}
