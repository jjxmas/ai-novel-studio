package com.jjxmas.ainovelstudio.pojo.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdeaResponse {

    private Long id;

    private String title;

    private List<String> sellingPoints;

    private String worldview;

    private String mainConflict;

    private Integer estimatedWordCount;

    private Integer longFormPotentialScore;

    private Integer conflictScore;

    private Integer noveltyScore;

    private Integer beginnerFriendlinessScore;

    private Integer platformFitScore;

    private String riskLevel;

    private String summary;

    private String status;
}

