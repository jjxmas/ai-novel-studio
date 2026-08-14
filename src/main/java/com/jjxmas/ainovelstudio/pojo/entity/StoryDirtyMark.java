package com.jjxmas.ainovelstudio.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jjxmas.ainovelstudio.common.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("story_dirty_marks")
public class StoryDirtyMark extends BaseEntity {

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

    private LocalDateTime resolvedAt;
}
