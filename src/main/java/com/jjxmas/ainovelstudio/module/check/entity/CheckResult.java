package com.jjxmas.ainovelstudio.module.check.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jjxmas.ainovelstudio.common.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 检查结果实体占位" */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("check_results")
public class CheckResult extends BaseEntity {

    private Long projectId;

    private Long chapterId;

    private Long jobId;

    private String checkType;

    private String severity;

    private String targetType;

    private Long targetId;

    private String issue;

    private String suggestion;

    private LocalDateTime resolvedAt;
}

