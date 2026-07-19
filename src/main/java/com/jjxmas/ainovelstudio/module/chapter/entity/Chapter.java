package com.jjxmas.ainovelstudio.module.chapter.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jjxmas.ainovelstudio.common.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 章节实体占位，后续可拆分章节大纲与正文版本" */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("chapters")
public class Chapter extends BaseEntity {

    private Long projectId;

    private Long volumeId;

    private Long storyArcId;

    private Integer chapterNo;

    private String title;

    private String outline;

    private String scenePlan;

    private String content;

    private Integer wordCount;

    private String status;

    private LocalDateTime confirmedOutlineAt;

    private LocalDateTime checkedAt;
}

