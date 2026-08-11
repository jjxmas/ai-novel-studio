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
public class OutlineResponse {

    private Long id;

    private Long projectId;

    private String outlineLevel;

    private String title;

    private String content;

    private Boolean confirmed;

    private List<VolumeOutlineResponse> volumes;
}
