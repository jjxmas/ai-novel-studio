package com.jjxmas.ainovelstudio.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.jjxmas.ainovelstudio.common.entity.BaseEntity;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "entity_state_records", autoResultMap = true)
public class EntityStateRecord extends BaseEntity {

    private Long projectId;

    private String entityType;

    private Long entityId;

    private String stateType;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> oldValue;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> newValue;

    private Long eventId;

    private Long chapterId;

    private LocalDateTime effectiveAt;
}
