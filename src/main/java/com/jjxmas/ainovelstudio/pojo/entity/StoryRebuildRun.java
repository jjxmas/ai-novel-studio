package com.jjxmas.ainovelstudio.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jjxmas.ainovelstudio.common.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("story_rebuild_runs")
public class StoryRebuildRun extends BaseEntity {

    private Long projectId;
    private Long generationJobId;
    private Long modelConfigId;
    private Integer requestedStartChapterNo;
    private Integer actualStartChapterNo;
    private String phase;
    private Integer nextFactChapterNo;
    private Integer nextMemoryChapterNo;
    private Boolean memoryResetDone;
    private Integer processedChapterCount;
    private Integer skippedChapterCount;

    @TableField("processed_chapter_nos_json")
    private String processedChapterNosJson;

    @TableField("skipped_chapter_nos_json")
    private String skippedChapterNosJson;

    @TableField("dirty_mark_ids_json")
    private String dirtyMarkIdsJson;

    private Integer activeDirtyMarkCountBefore;
    private String status;
    private String resultJson;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
