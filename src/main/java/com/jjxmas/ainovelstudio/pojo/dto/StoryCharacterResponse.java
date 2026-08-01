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
public class StoryCharacterResponse {

    private Long id;

    private Long projectId;

    private String name;

    private List<String> aliases;

    private String roleType;

    private String narrativeRole;

    private String identity;

    private String publicIdentity;

    private String gender;

    private String ageText;

    private String personality;

    private String motivation;

    private String background;

    private String coreGoal;

    private String innerNeed;

    private String coreFlaw;

    private String bottomLine;

    private String skillsSummary;

    private String secretNotes;

    private String relationshipSummary;

    private Integer importance;

    private String status;

    private Long firstAppearedChapterId;

    private String notes;
}
