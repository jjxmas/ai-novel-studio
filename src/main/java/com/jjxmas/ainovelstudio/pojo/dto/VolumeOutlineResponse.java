package com.jjxmas.ainovelstudio.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VolumeOutlineResponse {

    private Long id;

    private Integer volumeNo;

    private String title;

    private String summary;

    private String goal;

    private Integer estimatedWordCount;
}

