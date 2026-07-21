package com.jjxmas.ainovelstudio.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jjxmas.ainovelstudio.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 作品项目实体，保存小说项目的题材、目标字数、平台定位和当前流程状态。
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("projects")
public class Project extends BaseEntity {

    private String title;

    private String genres;

    private Integer targetWordCountMin;

    private Integer targetWordCountMax;

    private Integer targetChapterWordCount;

    private String platformTarget;

    private String stylePreference;

    private String projectBrief;

    private String status;

    private Long selectedIdeaId;
}
