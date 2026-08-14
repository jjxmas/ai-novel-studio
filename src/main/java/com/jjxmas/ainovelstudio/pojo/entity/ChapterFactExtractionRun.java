package com.jjxmas.ainovelstudio.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jjxmas.ainovelstudio.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("chapter_fact_extraction_runs")
public class ChapterFactExtractionRun extends BaseEntity {

    private Long projectId;

    private Long chapterId;

    private Long sourceContentVersionId;

    private Long modelConfigId;

    private String status;

    private String rawOutputJson;

    private String normalizedOutputJson;

    private String issuesJson;

    private Long generationJobId;
}
