package com.jjxmas.ainovelstudio.pojo.dto;

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

    private String summary;

    private String charactersSummary;

    private String locationsSummary;

    private String rulesSummary;

    private Boolean confirmed;
}

