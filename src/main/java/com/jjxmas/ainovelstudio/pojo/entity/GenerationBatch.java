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
@TableName("generation_batches")
public class GenerationBatch extends BaseEntity {

    private Long projectId;
    private String batchType;
    private Long modelConfigId;
    private String status;
    private Integer totalCount;
    private Integer pendingCount;
    private Integer runningCount;
    private Integer succeededCount;
    private Integer failedCount;
    private Integer skippedCount;
    private Integer qualityCheckedCount;
    private Integer qualityFailedCount;
    private Integer qualityIssueCount;
    private String requestSnapshot;
    private String errorMessage;
    private Long createdBy;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
