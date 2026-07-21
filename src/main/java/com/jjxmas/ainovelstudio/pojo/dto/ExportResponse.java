package com.jjxmas.ainovelstudio.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportResponse {

    private String fileName;

    private String filePath;

    private String format;

    private String scope;

    private String content;
}

