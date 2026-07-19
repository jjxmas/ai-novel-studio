package com.jjxmas.ainovelstudio.module.version.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jjxmas.ainovelstudio.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 内容版本快照实体占位" */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("content_versions")
public class ContentVersion extends BaseEntity {

    private Long projectId;

    private String entityType;

    private Long entityId;

    private Integer versionNo;

    private String snapshot;

    private String changeSource;

    private String operationType;

    private String changeNote;

    private String revisionInstruction;

    private Long modelConfigId;

    private Long jobId;
}

