package com.jjxmas.ainovelstudio.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jjxmas.ainovelstudio.common.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 设定库入口实体占位，人物/地点/规则后续可拆表" */
/**
 * 设定库实体，保存项目世界观、人物、地点和规则等统一设定内容。
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("setting_libraries")
public class SettingLibrary extends BaseEntity {

    private Long projectId;

    private String summary;

    private LocalDateTime confirmedAt;
}
