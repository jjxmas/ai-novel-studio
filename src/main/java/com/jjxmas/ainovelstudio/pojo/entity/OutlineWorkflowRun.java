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
@TableName("outline_workflow_runs")
public class OutlineWorkflowRun extends BaseEntity {

    private Long projectId;

    private Long settingLibraryId;

    private Long modelConfigId;

    private String status;

    private String draftJson;

    private String checkJson;

    private LocalDateTime committedAt;
}
