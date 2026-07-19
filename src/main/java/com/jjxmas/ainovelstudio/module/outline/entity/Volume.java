package com.jjxmas.ainovelstudio.module.outline.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jjxmas.ainovelstudio.common.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("volumes")
public class Volume extends BaseEntity {

    private Long projectId;

    private Integer volumeNo;

    private String title;

    private String summary;

    private String goal;

    private Integer estimatedWordCount;

    private LocalDateTime confirmedAt;
}

