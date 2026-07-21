package com.jjxmas.ainovelstudio.common.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 基础实体，提供所有业务表通用的 ID、创建时间和更新时间字段。
 */
@Data
@Accessors(chain = true)
public abstract class BaseEntity implements Serializable {

    private Long id;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
