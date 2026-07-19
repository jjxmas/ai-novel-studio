package com.jjxmas.ainovelstudio.module.outline.service;

import com.jjxmas.ainovelstudio.module.chapter.dto.ChapterResponse;
import com.jjxmas.ainovelstudio.module.outline.dto.OutlineGenerateRequest;
import com.jjxmas.ainovelstudio.module.outline.dto.OutlineResponse;
import com.jjxmas.ainovelstudio.module.outline.dto.OutlineRewriteRequest;
import com.jjxmas.ainovelstudio.module.outline.dto.OutlineUpdateRequest;
import java.util.List;

public interface OutlineService {

    OutlineResponse generateOutline(OutlineGenerateRequest request);

    OutlineResponse getGlobalOutline(Long projectId);

    OutlineResponse updateGlobalOutline(Long projectId, OutlineUpdateRequest request);

    OutlineResponse updateGlobalOutlineById(Long outlineId, OutlineUpdateRequest request);

    OutlineResponse rewriteGlobalOutline(Long projectId, OutlineRewriteRequest request);

    OutlineResponse confirmGlobalOutline(Long projectId);

    OutlineResponse confirmGlobalOutlineById(Long outlineId);

    List<ChapterResponse> generateChapterOutlines(Long projectId);
}

