package com.jjxmas.ainovelstudio.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.jjxmas.ainovelstudio.common.entity.BaseEntity;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "characters", autoResultMap = true)
public class StoryCharacter extends BaseEntity {

    private Long projectId;

    private String name;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> alias;

    private String roleType;

    private String narrativeRole;

    private String identity;

    private String publicIdentity;

    private String gender;

    private String ageText;

    private String personality;

    private String motivation;

    private String background;

    private String coreGoal;

    private String innerNeed;

    private String coreFlaw;

    private String bottomLine;

    private String skillsSummary;

    private String secretNotes;

    private String relationshipSummary;

    private Integer importance;

    private String status;

    private Long firstAppearedChapterId;

    private String notes;
}
