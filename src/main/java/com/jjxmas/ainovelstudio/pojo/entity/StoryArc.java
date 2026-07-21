package com.jjxmas.ainovelstudio.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jjxmas.ainovelstudio.common.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 剧情弧实体，保存卷内阶段剧情单元的目标、冲突和章节数量规划。
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("story_arcs")
public class StoryArc extends BaseEntity {

    private Long projectId;

    private Long volumeId;

    private Integer arcNo;

    private String title;

    private String summary;

    private String goal;

    private String conflict;

    private Integer estimatedChapterCount;

    private LocalDateTime confirmedAt;
}
