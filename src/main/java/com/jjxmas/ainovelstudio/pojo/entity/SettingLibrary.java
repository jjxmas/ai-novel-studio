package com.jjxmas.ainovelstudio.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jjxmas.ainovelstudio.common.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("setting_libraries")
public class SettingLibrary extends BaseEntity {

    private Long projectId;

    private Long sourceIdeaId;

    private String summary;

    private String overview;

    private String genreTemplate;

    private String status;

    private LocalDateTime confirmedAt;
}
