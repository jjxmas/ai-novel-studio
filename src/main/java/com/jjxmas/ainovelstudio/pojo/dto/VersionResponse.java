package com.jjxmas.ainovelstudio.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionResponse {

    private Long id;

    private String entityType;

    private Long entityId;

    private Integer versionNo;

    private String changeSource;

    private String changeNote;

    private String snapshot;
}

