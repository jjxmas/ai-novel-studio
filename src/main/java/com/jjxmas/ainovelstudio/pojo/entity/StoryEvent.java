package com.jjxmas.ainovelstudio.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jjxmas.ainovelstudio.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("story_events")
public class StoryEvent extends BaseEntity {

    private Long projectId;

    private String name;

    private String eventType;

    private String description;

    private String eventTimeText;

    private Long locationId;

    private Long chapterId;

    private Long sourceContentVersionId;

    private Boolean isPlanned;

    private Integer importance;
}
