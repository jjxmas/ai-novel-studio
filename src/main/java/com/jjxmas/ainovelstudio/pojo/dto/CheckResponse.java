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
public class CheckResponse {

    private Integer issueCount;

    private List<CheckIssueResponse> issues;

    private String summary;
}

