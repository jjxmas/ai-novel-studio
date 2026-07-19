package com.jjxmas.ainovelstudio.module.memory.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jjxmas.ainovelstudio.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 故事记忆快照，承载近窗、中层、高层和全局总摘要。
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("story_memories")
public class StoryMemory extends BaseEntity {

    private Long projectId;

    private String memoryType;

    private String memoryKey;

    private Integer sequenceNo;

    private Integer startChapterNo;

    private Integer endChapterNo;

    private String content;

    private String sourceChapterSummaryIds;

    private String sourceMemoryIds;

    private String status;

    @TableField("is_current")
    private Boolean current;

    private Integer compressionRound;

    private Long generationJobId;

    private Long contentVersionId;
}
