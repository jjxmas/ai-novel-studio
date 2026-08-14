package com.jjxmas.ainovelstudio.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jjxmas.ainovelstudio.common.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 大纲实体，保存项目全局大纲内容、标题和确认状态。
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("global_outlines")
public class Outline extends BaseEntity {

    private Long projectId;

    private String title;

    private String content;

    private LocalDateTime confirmedAt;
}
