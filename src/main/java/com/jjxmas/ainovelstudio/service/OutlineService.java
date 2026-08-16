package com.jjxmas.ainovelstudio.service;

import com.jjxmas.ainovelstudio.pojo.dto.OutlineResponse;
import com.jjxmas.ainovelstudio.pojo.dto.OutlineRewriteRequest;
import com.jjxmas.ainovelstudio.pojo.dto.OutlineUpdateRequest;

/**
 * 大纲服务，提供全局大纲和章节大纲相关业务能力。
 */
public interface OutlineService {

    /**
     * 查询指定项目的全局大纲。
     */
    OutlineResponse getGlobalOutline(Long projectId);

    /**
     * 按项目 ID 更新全局大纲。
     */
    void updateGlobalOutline(Long projectId, OutlineUpdateRequest request);

    /**
     * 按大纲 ID 更新全局大纲。
     */
    void updateGlobalOutlineById(Long outlineId, OutlineUpdateRequest request);

    /**
     * 根据重写指令重新生成全局大纲。
     */
    OutlineResponse rewriteGlobalOutline(Long projectId, OutlineRewriteRequest request);

    /**
     * 按项目 ID 确认全局大纲。
     */
    void confirmGlobalOutline(Long projectId);

    /**
     * 按大纲 ID 确认全局大纲。
     */
    void confirmGlobalOutlineById(Long outlineId);

}
