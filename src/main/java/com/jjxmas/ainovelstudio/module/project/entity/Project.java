package com.jjxmas.ainovelstudio.module.project.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jjxmas.ainovelstudio.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 作品项目主表实体占位" */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("projects")
public class Project extends BaseEntity {

    private String title;

    private String genres;

    private Integer targetWordCountMin;

    private Integer targetWordCountMax;

    private String platformTarget;

    private String stylePreference;

    private String projectBrief;

    private String status;

    private Long selectedIdeaId;
}

