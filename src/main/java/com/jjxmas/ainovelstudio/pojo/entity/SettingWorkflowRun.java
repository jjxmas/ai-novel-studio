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
@TableName("setting_workflow_runs")
public class SettingWorkflowRun extends BaseEntity {

    private Long projectId;

    private Long sourceIdeaId;

    private Long modelConfigId;

    private String status;

    private String blueprintJson;

    private String draftJson;

    private String checkJson;

    private LocalDateTime blueprintConfirmedAt;

    private LocalDateTime committedAt;
}
