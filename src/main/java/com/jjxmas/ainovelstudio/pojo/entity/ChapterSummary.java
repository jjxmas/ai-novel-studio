package com.jjxmas.ainovelstudio.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jjxmas.ainovelstudio.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 单章摘要，用于长篇章节上下文压缩。
 */
/**
 * 章节摘要实体，保存单章内容压缩后的摘要和关键要素。
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("chapter_summaries")
public class ChapterSummary extends BaseEntity {

    private Long projectId;

    private Long chapterId;

    private Integer chapterNo;

    private String summary;

    private String keyEvents;

    private String characterChanges;

    private String locationChanges;

    private String foreshadowChanges;

    private Long sourceContentVersionId;

    private Long summaryVersionId;

    private Long generationJobId;
}
