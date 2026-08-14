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
public class StoryRebuildResult {

    private Long projectId;

    private Integer requestedStartChapterNo;

    private Integer actualStartChapterNo;

    private Integer endChapterNo;

    private Integer processedChapterCount;

    private Integer skippedChapterCount;

    private Integer activeDirtyMarkCountBefore;

    private Integer resolvedDirtyMarkCount;

    private Integer activeDirtyMarkCountAfter;

    private Integer earliestDirtyChapterNoAfter;

    private List<Integer> processedChapterNos;

    private List<Integer> skippedChapterNos;

    private String status;

    private String note;
}
