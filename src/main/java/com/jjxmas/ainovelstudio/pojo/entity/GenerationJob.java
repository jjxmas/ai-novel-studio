package com.jjxmas.ainovelstudio.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jjxmas.ainovelstudio.common.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 生成任务实体，记录 AI 生成或检查任务的输入、输出、状态和耗时信息。
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("generation_jobs")
public class GenerationJob extends BaseEntity {

    private Long projectId;

    private String jobType;

    private String relatedEntityType;

    private Long relatedEntityId;

    private Long modelConfigId;

    private String status;

    private Integer priority;

    private Integer attemptCount;

    private String inputSnapshot;

    private String outputSnapshot;

    private String errorMessage;

    private String lockedBy;

    private LocalDateTime lockedAt;

    private LocalDateTime scheduledAt;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;
}
