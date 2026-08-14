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
public class StoryDirtyMarkSnapshotResponse {

    private Long projectId;

    private Integer queryChapterNo;

    private Integer activeDirtyMarkCount;

    private Integer earliestDirtyChapterNo;

    private List<StoryDirtyMarkResponse> activeDirtyMarks;
}
