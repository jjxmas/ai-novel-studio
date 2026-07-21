package com.jjxmas.ainovelstudio.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jjxmas.ainovelstudio.common.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 分卷实体，保存作品分卷编号、标题、阶段目标和预估字数。
 */
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
