package com.jjxmas.ainovelstudio.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jjxmas.ainovelstudio.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("chapter_post_process_runs")
public class ChapterPostProcessRun extends BaseEntity {

    private Long projectId;
    private Long chapterId;
    private Integer contentVersionNo;
    private Long generationJobId;
    private String status;
    private Integer completedStep;
    private String qualityStatus;
    private Integer qualityIssueCount;
    private String qualityErrorMessage;
    private String errorMessage;
}
