package com.jjxmas.ainovelstudio.common.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 基础实体字段。数据库迁移文件落定后，再按表结构补充自动填充策略。
 */
@Data
@Accessors(chain = true)
public abstract class BaseEntity implements Serializable {

    private Long id;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
