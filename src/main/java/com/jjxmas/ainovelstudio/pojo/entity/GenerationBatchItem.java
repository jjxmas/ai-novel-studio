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
@TableName("generation_batch_items")
public class GenerationBatchItem extends BaseEntity {

    private Long batchId;
    private Long projectId;
    private Long chapterId;
    private Integer chapterNo;
    private String itemType;
    private String status;
    private Integer attemptCount;
    private Long generationJobId;
    private String qualityStatus;
    private Integer qualityIssueCount;
    private String qualityReport;
    private String qualityErrorMessage;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
