package com.jjxmas.ainovelstudio.module.idea.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jjxmas.ainovelstudio.common.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 创意方案实体占位" */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("ideas")
public class Idea extends BaseEntity {

    private Long projectId;

    private String title;

    private String sellingPoints;

    private String worldview;

    private String mainConflict;

    private Integer estimatedWordCount;

    private String summary;

    private String status;

    private Long modelConfigId;

    private LocalDateTime selectedAt;

    private LocalDateTime rejectedAt;
}

