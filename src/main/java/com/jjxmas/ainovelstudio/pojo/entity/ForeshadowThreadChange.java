package com.jjxmas.ainovelstudio.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jjxmas.ainovelstudio.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("foreshadow_thread_changes")
public class ForeshadowThreadChange extends BaseEntity {

    private Long projectId;

    private String threadKey;

    private String threadTitle;

    private String threadType;

    private String changeKind;

    private String changeType;

    private Integer priority;

    private Long chapterId;

    private Integer chapterNo;

    private Long originChapterId;

    private Integer originChapterNo;

    private Long sourceContentVersionId;

    private String setupText;

    private String progressText;

    private String payoffHint;

    private Integer targetPayoffChapterNo;
}
