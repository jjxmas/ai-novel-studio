package com.jjxmas.ainovelstudio.module.setting.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jjxmas.ainovelstudio.common.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 设定库入口实体占位，人物/地点/规则后续可拆表" */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("setting_libraries")
public class SettingLibrary extends BaseEntity {

    private Long projectId;

    private String summary;

    private LocalDateTime confirmedAt;
}

