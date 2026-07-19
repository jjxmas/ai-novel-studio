package com.jjxmas.ainovelstudio.module.idea.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.experimental.Accessors;

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

