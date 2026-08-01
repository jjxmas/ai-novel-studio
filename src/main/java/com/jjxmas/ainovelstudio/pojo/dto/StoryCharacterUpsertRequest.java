package com.jjxmas.ainovelstudio.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.Data;

@Data
public class StoryCharacterUpsertRequest {

    @NotBlank(message = "角色名称不能为空")
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
