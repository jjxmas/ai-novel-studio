package com.jjxmas.ainovelstudio.pojo.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 服务端章节分页结果。
 */
@Data
@Builder
public class ChapterPageResponse {

    private List<ChapterCatalogResponse> items;
    private Long total;
    private Integer page;
    private Integer size;
}
