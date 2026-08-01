package com.jjxmas.ainovelstudio.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.jjxmas.ainovelstudio.common.entity.BaseEntity;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 创意实体，保存项目候选创意的卖点、世界观、主冲突、摘要和选中状态。
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value ="ideas",autoResultMap = true)
public class Idea extends BaseEntity {
   // private Long id;

    private Long projectId;

    private String title;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> sellingPoints;

    private String worldview;

    private String mainConflict;

    private Integer estimatedWordCount;

    private String summary;

    private String status;

    private Long modelConfigId;

    private LocalDateTime selectedAt;

    private LocalDateTime rejectedAt;
}
