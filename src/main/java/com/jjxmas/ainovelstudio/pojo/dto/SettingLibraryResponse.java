package com.jjxmas.ainovelstudio.pojo.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettingLibraryResponse {

    private Long id;

    private Long projectId;

    private Long sourceIdeaId;

    private String summary;

    private String overview;

    private String genreTemplate;

    private String status;

    private Boolean confirmed;

    private LocalDateTime confirmedAt;

    private Integer characterCount;

    private Integer organizationCount;

    private Integer locationCount;

    private Integer itemCount;

    private Integer ruleCount;

    private Integer relationCount;

    private Integer eventCount;

    private Integer stateRecordCount;

    private Integer completenessScore;
}
