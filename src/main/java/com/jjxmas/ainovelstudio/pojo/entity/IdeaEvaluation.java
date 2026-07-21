package com.jjxmas.ainovelstudio.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 创意评估实体，保存创意在长篇潜力、冲突、新意和平台适配等维度的评分。
 */
@Data
@Accessors(chain = true)
@TableName("idea_evaluations")
public class IdeaEvaluation {

    private Long id;

    private Long ideaId;

    private Integer roundNo;

    private Double longFormPotentialScore;

    private Double conflictScore;

    private Double noveltyScore;

    private Double beginnerFriendlinessScore;

    private Double platformFitScore;

    private String riskLevel;

    private String strengths;

    private String risks;

    private String suggestions;

    private String overallComment;

    private Long modelConfigId;

    private LocalDateTime createdAt;
}
