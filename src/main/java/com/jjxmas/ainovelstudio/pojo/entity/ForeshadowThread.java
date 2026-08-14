package com.jjxmas.ainovelstudio.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.jjxmas.ainovelstudio.common.entity.BaseEntity;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "foreshadow_threads", autoResultMap = true)
public class ForeshadowThread extends BaseEntity {

    private Long projectId;

    private String threadKey;

    private String threadTitle;

    private String threadType;

    private String status;

    private Integer priority;

    private Long sourceChapterId;

    private Integer sourceChapterNo;

    private Long lastMentionedChapterId;

    private Integer lastMentionedChapterNo;

    private Integer targetPayoffChapterNo;

    private Long resolutionChapterId;

    private Integer resolutionChapterNo;

    private String setupText;

    private String latestProgress;

    private String payoffHint;

    private String resolutionNote;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> relatedCharacterIds;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> relatedOrganizationIds;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> relatedLocationIds;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> relatedItemIds;

    private String notes;
}
