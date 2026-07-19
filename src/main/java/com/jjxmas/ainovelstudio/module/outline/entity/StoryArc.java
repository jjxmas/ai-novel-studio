package com.jjxmas.ainovelstudio.module.outline.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jjxmas.ainovelstudio.common.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("story_arcs")
public class StoryArc extends BaseEntity {

    private Long projectId;

    private Long volumeId;

    private Integer arcNo;

    private String title;

    private String summary;

    private String goal;

    private String conflict;

    private Integer estimatedChapterCount;

    private LocalDateTime confirmedAt;
}

